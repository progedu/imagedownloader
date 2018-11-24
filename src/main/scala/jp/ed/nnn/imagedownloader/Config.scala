package jp.ed.nnn.imagedownloader

case class Config
(
  wordsFilePath: String,
  urlsFilePath: String,
  outputDirPath: String,
  numOfDownloader: Int
)
