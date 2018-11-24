package jp.ed.nnn.imagedownloader

import akka.actor.Actor

import scala.io.{Codec, Source}

class UrlsFileLoader(config: Config) extends Actor {

  override def receive: Receive = {
    case LoadUrlsFile =>
      val urlsFileSource = Source.fromFile(config.urlsFilePath)(Codec.UTF8)
      val urlsIterator = urlsFileSource.getLines()
      urlsIterator.foreach { line =>
        val Array(id, url) = line.split("\t")
        val wnid = id.split("_").head
        val imageNetUrl = ImageNetUrl(id, url, wnid)
        sender() ! imageNetUrl
      }
      urlsFileSource.close()
  }

}
