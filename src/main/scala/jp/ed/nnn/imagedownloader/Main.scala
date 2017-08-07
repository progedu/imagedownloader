package jp.ed.nnn.imagedownloader

import akka.actor.{ActorSystem, Inbox, Props}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  // TODO please fix to your configuration
  val wordsFilePath = "/Users/soichiro_yoshimura/Desktop/ImageUrls/words.txt"
  val urlsFilePath = "/Users/soichiro_yoshimura/Desktop/ImageUrls/fall11_urls_100.txt"
  val outputDirPath = "/Users/soichiro_yoshimura/Desktop/imagenet_download"
  val numOfDownloader = 2000
  val config = Config(
    wordsFilePath,
    urlsFilePath,
    outputDirPath,
    numOfDownloader)

  val system = ActorSystem("imagedownloader")
  val inbox = Inbox.create(system)
  implicit val sender = inbox.getRef()

  val supervisor = system.actorOf(Props(new Supervisor(config)))
  supervisor ! Start

  inbox.receive(100.days)
  Await.ready(system.terminate(), Duration.Inf)
  println("Finished.")
}
