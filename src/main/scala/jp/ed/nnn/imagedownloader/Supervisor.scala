package jp.ed.nnn.imagedownloader

import java.util.concurrent.TimeUnit

import okhttp3._

import scala.io.{Codec, Source}

class Supervisor(implicit config: Config) {

  private val wordsFileSource = Source.fromFile(config.wordsFilePath)
  val wnidWordMap: Map[String, String] = wordsFileSource.getLines().map(s => {
    val strs = s.split("\t")
    (strs.head, strs.tail.mkString("\t"))
  }).toMap


  val client: OkHttpClient = new OkHttpClient.Builder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .writeTimeout(1, TimeUnit.SECONDS)
    .readTimeout(1, TimeUnit.SECONDS)
    .build()

  val urlsFilePath = Source.fromFile(config.urlsFilePath)(Codec.UTF8).getLines()
}

object Supervisor {
  def apply()(implicit config: Config): Supervisor = new Supervisor
}
