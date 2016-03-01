package services

import models.order._
import models.rma.{Rmas, Rma}
import OrderPayments.scope._
import responses.{AllRmas, TheResponse}
import utils.Slick.implicits._
import utils.Slick.DbResult
import utils.aliases._

package object rmas {
  object Helpers {
    def rmaNotFound(refNum: String): NotFoundFailure404 = NotFoundFailure404(Rma, refNum)

    def mustFindPendingRmaByRefNum(refNum: String)(implicit ec: EC): DbResult[Rma] =
      Rmas.findOnePendingByRefNum(refNum).mustFindOr(rmaNotFound(refNum))

    def mustFindCcPaymentsByOrderId(orderId: Int)(implicit ec: EC): DbResult[OrderPayment] =
      OrderPayments.findAllByOrderId(orderId).creditCards
        .one.mustFindOr(OrderPaymentNotFoundFailure(Order))
  }

  type BulkRmaUpdateResponse = TheResponse[Seq[AllRmas.Root]]
}
