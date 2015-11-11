package services

import scala.concurrent.ExecutionContext

import models.{Rma, Rmas}
import utils.Slick.implicits._
import utils.Slick.DbResult

package object rmas {
  object Helpers {
    def rmaNotFound(refNum: String): NotFoundFailure404 = NotFoundFailure404(Rma, refNum)

    def mustFindRmaByRefNum(refNum: String)(implicit ec: ExecutionContext): DbResult[Rma] =
      Rmas.findOneByRefNum(refNum).mustFindOr(rmaNotFound(refNum))

    def mustFindPendingRmaByRefNum(refNum: String)(implicit ec: ExecutionContext): DbResult[Rma] =
      Rmas.findOnePendingByRefNum(refNum).mustFindOr(rmaNotFound(refNum))
  }
}