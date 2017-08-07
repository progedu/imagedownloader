package jp.ed.nnn.imagedownloader

trait Message

sealed trait SupervisorMessage extends Message
case object Start extends SupervisorMessage
case object Finished extends SupervisorMessage
case class DownloadSuccess(tmpFilePath: String, imageNetUrl: ImageNetUrl) extends SupervisorMessage
case class DownloadFailure(e: Throwable, imageNetUrl: ImageNetUrl) extends SupervisorMessage

sealed trait UrlsFileLoaderMessage extends Message
case object LoadUrlsFile extends UrlsFileLoaderMessage

sealed trait ImageFileDownloaderMessage extends Message

case class ImageNetUrl(id: String, url: String, wnid: String)
  extends ImageFileDownloaderMessage with SupervisorMessage
