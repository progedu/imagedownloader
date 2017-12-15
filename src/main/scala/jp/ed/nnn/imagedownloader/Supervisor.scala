package jp.ed.nnn.imagedownloader

import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.routing.RoundRobinPool
import akka.stream.{ActorMaterializer, OverflowStrategy, ThrottleMode}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.Timeout
import okhttp3._

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._
import scala.io.{Codec, Source => IOSource}

class Supervisor(config: Config) extends Actor {
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val askTimeout: Timeout = Timeout(100.days)
  var originalSender: ActorRef = Actor.noSender

  var successCount = 0
  var failureCount = 0

  val wordsFileSource: IOSource = IOSource.fromFile(config.wordsFilePath)
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

  val ref: ActorRef = context.actorOf(
    Props(new ImageFileDownloader(config, client, wnidWordMap))
      .withRouter(new RoundRobinPool(config.numOfDownloader)))

  val urlsFileSource: IOSource =
    IOSource.fromFile(config.urlsFilePath)(Codec.UTF8)
  val source: Source[String, NotUsed] =
    Source.fromIterator(() => urlsFileSource.getLines())

  override def receive: Receive = {
    case Start =>
      originalSender = sender()
      source
        .via(convertToImageNetUrl)
        .throttle(100, 1.second, 10, ThrottleMode.shaping)
        .buffer(config.numOfDownloader, OverflowStrategy.backpressure)
        .via(askDownload)
        .via(countUp)
        .runForeach(_ => printConsole())
        .onComplete(_ => originalSender ! Finished)
  }

  private[this] def convertToImageNetUrl: Flow[String, ImageNetUrl, NotUsed] =
    Flow[String].map { line =>
      val Array(id, url) = line.split("\t")
      val wnid = id.split("_").head
      ImageNetUrl(id, url, wnid)
    }

  private[this] def askDownload: Flow[ImageNetUrl, Message, NotUsed] =
    Flow[ImageNetUrl]
      .mapAsyncUnordered(config.numOfDownloader)(i => (ref ? i).mapTo[Message])

  private[this] def countUp: Flow[Message, Unit, NotUsed] =
    Flow[Message].map {
      case DownloadSuccess(_, _) => successCount += 1
      case DownloadFailure(_, _) => failureCount += 1
    }

  private[this] def printConsole(): Unit = {
    val total = successCount + failureCount
    println(s"total: $total, successCount: $successCount, failureCount: $failureCount")
  }
}
