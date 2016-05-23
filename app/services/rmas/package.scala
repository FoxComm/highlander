package services

import models.order._
import models.rma.{Rma, Rmas}
import OrderPayments.scope._
import failures.NotFoundFailure404
import failures.OrderFailures.OrderPaymentNotFoundFailure
import utils.db._
import utils.aliases._

package object rmas {
  object Helpers {
    def rmaNotFound(refNum: String): NotFoundFailure404 = NotFoundFailure404(Rma, refNum)

    def mustFindPendingRmaByRefNum(refNum: String)(implicit ec: EC): DbResult[Rma] =
      Rmas.findOnePendingByRefNum(refNum).mustFindOr(rmaNotFound(refNum))

    def mustFindCcPaymentsByOrderId(orderId: Int)(implicit ec: EC): DbResult[OrderPayment] =
      OrderPayments.findAllByOrderId(orderId).creditCards
        .mustFindOneOr(OrderPaymentNotFoundFailure(Order))
  }
}
