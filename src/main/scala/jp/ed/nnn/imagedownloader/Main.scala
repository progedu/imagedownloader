package jp.ed.nnn.imagedownloader

import java.io.{File, FileReader}

import akka.actor.{ActorSystem, Inbox, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  // TODO please fix to your configuration
  val wordsFilePath = "/Users/user/Downloads/words.txt"
  val urlsFilePath = "/Users/user/Downloads/fall11_urls.txt"
  val outputDirPath = "/Users/user/Downloads/imagenet_download"
  val numOfDownloader = 10
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
