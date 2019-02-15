package jp.ed.nnn.imagedownloader

import akka.NotUsed
import akka.stream.scaladsl._

class UrlsFileLoader(implicit config: Config) {

  def getFlow: Flow[String, ImageNetUrl, NotUsed] = {
    Flow[String].map( s => {
      val strs = s.split("\t")
      val id = strs.head
      val url = strs.tail.mkString("\t")
      val wnid = id.split("_").head
      ImageNetUrl(id, url, wnid)
    })
  }

}

object UrlsFileLoader {
  def apply()(implicit config: Config): UrlsFileLoader = new UrlsFileLoader
}
