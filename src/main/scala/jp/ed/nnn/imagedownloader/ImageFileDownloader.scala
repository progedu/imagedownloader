package jp.ed.nnn.imagedownloader

import java.io.{File, IOException}
import java.nio.file.{Files, Paths, StandardOpenOption}

import akka.NotUsed
import akka.stream.scaladsl._
import okhttp3._

import scala.util.{Failure, Success, Try}

class DownloadFailException extends IOException

class ImageFileDownloader(client: OkHttpClient, wnidWordMap: Map[String, String])(implicit config: Config) {
  private val jpegMediaType = MediaType.parse("image/jpeg")

  def getFlow: Flow[ImageNetUrl, DownloadResult, NotUsed] = {
    Flow[ImageNetUrl].map(i => {
      var result: DownloadResult = NotResult

      val request = new Request.Builder()
        .url(i.url)
        .build()

      Try { client.newCall(request).execute() } match {
        case Failure(e) =>
          result = DownloadFailure(e, i)

        case Success(res) =>
          if (res.isSuccessful
            && jpegMediaType == res.body().contentType()) {

            val dir = new File(new File(config.outputDirPath),
              i.wnid + "-" + wnidWordMap(i.wnid))
            dir.mkdir()

            val downloadFile = new File(dir, i.id + ".jpg")
            if (!downloadFile.exists()) downloadFile.createNewFile()

            val tmpFilePath = Paths.get(downloadFile.getAbsolutePath)
            Try {
              Files.write(tmpFilePath, res.body().bytes(), StandardOpenOption.WRITE)
            } match {
              case Success(_) => result = DownloadSuccess(downloadFile.getAbsolutePath, i)
              case Failure(e) =>
                downloadFile.delete()
                result = DownloadFailure(e, i)
            }
          } else result = DownloadFailure(new DownloadFailException, i)
          res.close()
      }

      result
    })
  }

}

object ImageFileDownloader {
  def apply(supervisor: Supervisor)(implicit config: Config): ImageFileDownloader =
    new ImageFileDownloader(supervisor.client, supervisor.wnidWordMap)
}
