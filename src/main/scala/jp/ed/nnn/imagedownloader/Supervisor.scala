package jp.ed.nnn.imagedownloader

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import okhttp3._

import scala.io.Source

class Supervisor(config: Config) extends Actor {
  var originalSender: ActorRef = Actor.noSender

  var successCount = 0
  var failureCount = 0
  var fileLoadedUrlCount = 0

  val wordsFileSource: Source = Source.fromFile(config.wordsFilePath)
  val wnidWordMap: Map[String, String] = wordsFileSource
    .getLines()
    .map { s =>
      val Array(wnid, word) = s.split("\t")
      wnid -> word
    }
    .toMap

  val client: OkHttpClient = new OkHttpClient.Builder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .writeTimeout(1, TimeUnit.SECONDS)
    .readTimeout(1, TimeUnit.SECONDS)
    .build()

  val router: Router = {
    val props = Props(new ImageFileDownloader(config, client, wnidWordMap))
    val downloaders = Vector.fill(config.numOfDownloader) {
      ActorRefRoutee(context.actorOf(props))
    }
    Router(RoundRobinRoutingLogic(), downloaders)
  }

  override def receive: Receive = {
    case Start =>
      originalSender = sender()
      val urlsFileLoader = context.actorOf(Props(new UrlsFileLoader(config)))
      urlsFileLoader ! LoadUrlsFile

    case imageNetUrl: ImageNetUrl =>
      fileLoadedUrlCount += 1
      router.route(imageNetUrl, self)

    case DownloadSuccess(_, _) =>
      successCount += 1
      printConsoleAndCheckFinish()

    case DownloadFailure(_, _) =>
      failureCount += 1
      printConsoleAndCheckFinish()
  }

  private[this] def printConsoleAndCheckFinish(): Unit = {
    val total = successCount + failureCount
    println(s"total: $total, successCount: $successCount, failureCount: $failureCount")
    if (total == fileLoadedUrlCount) originalSender ! Finished
  }
}
