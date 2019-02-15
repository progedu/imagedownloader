package jp.ed.nnn.imagedownloader

import akka.NotUsed
import akka.stream.scaladsl._

class DownloadCounter {

  def getFlow: Flow[DownloadResult, Unit, NotUsed] = {
    Flow[DownloadResult].statefulMapConcat{ () =>
      var successCount = 0
      var failureCount = 0
      var totalCount = 0

      result => {
        result match {
          case DownloadSuccess(_, _) => {
            successCount += 1
            totalCount += 1
            println(s"Total: $totalCount success: $successCount failure: $failureCount")
          }
          case DownloadFailure(_, _) => {
            failureCount += 1
            totalCount += 1
            println(s"Total: $totalCount success: $successCount failure: $failureCount")

          }
          case NotResult =>
            totalCount += 1
            println("NotResult")

        }

        List()
      }
    }
  }
}

object DownloadCounter {
  def apply(): DownloadCounter = new DownloadCounter()
}
