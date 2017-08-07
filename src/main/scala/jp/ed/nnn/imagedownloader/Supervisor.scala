package jp.ed.nnn.imagedownloader

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import akka.routing.{ActorRefRoutee, Broadcast, RoundRobinRoutingLogic, Router}
import okhttp3._

import scala.io.Source


class Supervisor(config: Config) extends Actor {

  var originalSender = Actor.noSender

  var successCount = 0
  var failureCount = 0
  var finishCount = 0

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

  val urlsFileLoader = context.actorOf(Props(new UrlsFileLoader(config)))

  val router = {
    val downloaders = Vector.fill(config.numOfDownloader) {
      ActorRefRoutee(context.actorOf(
        Props(new ImageFileDownloader(
          config,
          client,
          wnidWordMap,
          urlsFileLoader
        ))))
    }
    Router(RoundRobinRoutingLogic(), downloaders)
  }

  override def receive = {

    case Start => {
      originalSender = sender()
      router.route(Broadcast(DownloadImage), self)
    }

    case DownloadSuccess(path, imageNetUrl) => {
      successCount += 1
      printConsole()
    }

    case DownloadFailure(e, imageNetUrl) => {
      failureCount += 1
      printConsole()
    }

    case Finished => {
      finishCount += 1
      if (finishCount == config.numOfDownloader) originalSender ! Finished
    }
  }

  private[this] def printConsole(): Unit = {
    val total = successCount + failureCount
    println(s"total: ${total}, successCount: ${successCount}, failureCount: ${failureCount}")
  }
}
