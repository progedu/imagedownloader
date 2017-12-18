package jp.ed.nnn.imagedownloader

import akka.actor.{ActorRef, ActorSystem, Inbox, Props}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {
  val wordsFilePath = "../../imagedownloader_texts/words.txt"
  val urlsFilePath = "../../imagedownloader_texts/fall11_urls.txt"
  val outputDirPath = "../../imagedownloader_download"
  val numOfDownloader = 2000
  val config = Config(
    wordsFilePath,
    urlsFilePath,
    outputDirPath,
    numOfDownloader)

  val system = ActorSystem("imagedownloader")
  val inbox = Inbox.create(system)
  implicit val sender: ActorRef = inbox.getRef()

  val supervisor = system.actorOf(Props(new Supervisor(config)))
  supervisor ! Start

  inbox.receive(100.days)
  Await.ready(system.terminate(), Duration.Inf)
  println("Finished.")
}
