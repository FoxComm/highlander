package services

import scala.concurrent.ExecutionContext

import models.{Order, Orders}
import utils.Slick.implicits._
import utils.Slick.DbResult

package object orders {
  object Helpers {
    def orderNotFound(refNum: String): NotFoundFailure404 = NotFoundFailure404(Order, refNum)

    def mustFindOrderByRefNum(refNum: String)(implicit ec: ExecutionContext): DbResult[Order] =
      Orders.findOneByRefNum(refNum).mustFindOr(orderNotFound(refNum))
  }
}
