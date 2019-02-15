package jp.ed.nnn.imagedownloader

sealed trait Message

case class ImageNetUrl(id: String, url: String, wnid: String) extends Message

sealed trait DownloadResult extends Message
case class DownloadSuccess(tmpFilePath: String, imageNetUrl: ImageNetUrl) extends DownloadResult
case class DownloadFailure(e: Throwable, imageNetUrl: ImageNetUrl) extends DownloadResult
case object NotResult extends DownloadResult
