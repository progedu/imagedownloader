package jp.ed.nnn.imagedownloader

import akka.actor.ActorSystem

import akka.stream._
import akka.stream.scaladsl._

object Main extends App {
  // TODO please fix to your configuration
  val wordsFilePath = "/.../.../workspace/imagedawnloaderUtils/util/words.txt"
  val urlsFilePath = "/.../.../workspace/imagedawnloaderUtils/util/fall11_urls.txt"
  val outputDirPath = "/.../.../workspace/imagedawnloaderUtils/images"
  val numOfDownloader = 20
  implicit val config: Config = Config(
    wordsFilePath,
    urlsFilePath,
    outputDirPath,
    numOfDownloader)

  implicit val system = ActorSystem("imagedownloader")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val supervisor = Supervisor()
  val fileLoader = UrlsFileLoader().getFlow
  val downloader = ImageFileDownloader(supervisor).getFlow
  val counter = DownloadCounter().getFlow

  val source = Source.fromIterator(() => supervisor.urlsFilePath)
  val sink = Sink.onComplete(_ => system.terminate())

  val parallelism = Flow.fromGraph(GraphDSL.create() {implicit builder =>
    import GraphDSL.Implicits._
    val balance = builder.add(Balance[ImageNetUrl](config.numOfDownloader))
    val merge = builder.add(Merge[DownloadResult](config.numOfDownloader))

    for (i <- 1 to config.numOfDownloader) {
      balance ~> downloader.async ~> merge
    }

    FlowShape(balance.in, merge.out)
  })

  val runnable = source via fileLoader via parallelism via counter to sink
  runnable.run()

}