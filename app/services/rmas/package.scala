package services

import scala.concurrent.ExecutionContext

import models.order._
import models.rma.{Rmas, Rma}
import OrderPayments.scope._
import responses.{AllRmas, TheResponse}
import utils.Slick.implicits._
import utils.Slick.DbResult

package object rmas {
  object Helpers {
    def rmaNotFound(refNum: String): NotFoundFailure404 = NotFoundFailure404(Rma, refNum)

    def mustFindPendingRmaByRefNum(refNum: String)(implicit ec: ExecutionContext): DbResult[Rma] =
      Rmas.findOnePendingByRefNum(refNum).mustFindOr(rmaNotFound(refNum))

    def mustFindCcPaymentsByOrderId(orderId: Int)(implicit ec: ExecutionContext): DbResult[OrderPayment] =
      OrderPayments.findAllByOrderId(orderId).creditCards
        .one.mustFindOr(OrderPaymentNotFoundFailure(Order))
  }

  type BulkRmaUpdateResponse = TheResponse[Seq[AllRmas.Root]]
}
