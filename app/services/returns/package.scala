package services

import models.order._
import models.returns._
import OrderPayments.scope._
import failures.NotFoundFailure404
import failures.OrderFailures.OrderPaymentNotFoundFailure
import utils.db._
import utils.aliases._

package object returns {
  object Helpers {
    def returnNotFound(refNum: String): NotFoundFailure404 = NotFoundFailure404(Return, refNum)

    def mustFindPendingReturnByRefNum(refNum: String)(implicit ec: EC): DbResult[Return] =
      Returns.findOnePendingByRefNum(refNum).mustFindOr(returnNotFound(refNum))

    def mustFindCcPaymentsByOrderId(orderId: Int)(implicit ec: EC): DbResult[OrderPayment] =
      OrderPayments
        .findAllByOrderId(orderId)
        .creditCards
        .mustFindOneOr(OrderPaymentNotFoundFailure(Order))
  }
}
