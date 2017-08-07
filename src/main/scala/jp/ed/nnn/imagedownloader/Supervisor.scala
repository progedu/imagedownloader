package jp.ed.nnn.imagedownloader

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import okhttp3._

import scala.io.Source


class Supervisor(config: Config) extends Actor {

  var originalSender = Actor.noSender

  var successCount = 0
  var failureCount = 0
  var fileLoadedUrlCount = 0

  val wordsFileSource = Source.fromFile(config.wordsFilePath)
  val wnidWordMap = wordsFileSource.getLines().map(s => {
    val strs = s.split("\t")
    (strs.head, strs.tail.mkString("\t"))
  }).toMap


  val client = new OkHttpClient.Builder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .writeTimeout(1, TimeUnit.SECONDS)
    .readTimeout(1, TimeUnit.SECONDS)
    .build()

  val router = {
    val downloaders = Vector.fill(config.numOfDownloader) {
      ActorRefRoutee(context.actorOf(
        Props(new ImageFileDownloader(
          config,
          client,
          wnidWordMap
        ))))
    }
    Router(RoundRobinRoutingLogic(), downloaders)
  }

  override def receive = {

    case Start => {
      originalSender = sender()
      val urlsFileLoader = context.actorOf(Props(new UrlsFileLoader(config)))
      urlsFileLoader ! LoadUrlsFile
    }

    case imageNetUrl: ImageNetUrl => {
      fileLoadedUrlCount += 1
      router.route(imageNetUrl, self)
    }

    case DownloadSuccess(path, imageNetUrl) => {
      successCount += 1
      printConsoleAndCheckFinish()
    }

    case DownloadFailure(e, imageNetUrl) => {
      failureCount += 1
      printConsoleAndCheckFinish()
    }
  }

  private[this] def printConsoleAndCheckFinish(): Unit = {
    val total = successCount + failureCount
    println(s"total: ${total}, successCount: ${successCount}, failureCount: ${failureCount}")
    if (total == fileLoadedUrlCount) originalSender ! Finished
  }
}
