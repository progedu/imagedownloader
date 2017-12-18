package jp.ed.nnn.imagedownloader

import java.io.{File, IOException}
import java.nio.file.{Files, Paths, StandardOpenOption}

import akka.actor.{Actor, ActorRef}
import okhttp3._

import scala.util.{Failure, Success, Try}

class DownloadFailException extends IOException

class ImageFileDownloader
(
  config: Config,
  client: OkHttpClient,
  wnidWordMap: Map[String, String]
)
  extends Actor {

  val jpegMediaType: MediaType = MediaType.parse("image/jpeg")
  var originalSender: ActorRef = Actor.noSender

  override def receive: Receive = {

    case imageNetUrl: ImageNetUrl =>
      originalSender = sender()

      val request = new Request.Builder()
        .url(imageNetUrl.url)
        .build()

      client.newCall(request).enqueue(new Callback {
        override def onFailure(call: Call, e: IOException): Unit =
          originalSender ! DownloadFailure(e, imageNetUrl)

        override def onResponse(call: Call, response: Response): Unit = {
          if (response.isSuccessful && jpegMediaType == response.body().contentType()) {

            val dir = new File(
              new File(config.outputDirPath),
              imageNetUrl.wnid + "-" + wnidWordMap(imageNetUrl.wnid)
            )
            dir.mkdir()

            val downloadFile = new File(dir, imageNetUrl.id + ".jpg")
            if (!downloadFile.exists()) downloadFile.createNewFile()

            val tmpFilePath = Paths.get(downloadFile.getAbsolutePath)

            Try {
              Files.write(tmpFilePath, response.body().bytes(), StandardOpenOption.WRITE)
            } match {
              case Success(_) =>
                originalSender ! DownloadSuccess(downloadFile.getAbsolutePath, imageNetUrl)

              case Failure(e) =>
                downloadFile.delete()
                originalSender ! DownloadFailure(e, imageNetUrl)
            }

          } else {
            originalSender ! DownloadFailure(new DownloadFailException, imageNetUrl)
          }
          response.close()
        }
      })

  }

}
