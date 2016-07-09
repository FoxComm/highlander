package services

import failures.NotFoundFailure404
import failures.OrderFailures.OrderPaymentNotFoundFailure
import models.cord.OrderPayments.scope._
import models.cord._
import models.returns._
import utils.aliases._
import utils.db._

package object returns {
  object Helpers {
    def returnNotFound(refNum: String): NotFoundFailure404 = NotFoundFailure404(Return, refNum)

    def mustFindPendingReturnByRefNum(refNum: String)(implicit ec: EC): DbResultT[Return] =
      Returns.findOnePendingByRefNum(refNum).mustFindOr(returnNotFound(refNum))

    def mustFindCcPaymentsByOrderRef(cordRef: String)(implicit ec: EC): DbResultT[OrderPayment] =
      OrderPayments
        .findAllByOrderRef(cordRef)
        .creditCards
        .mustFindOneOr(OrderPaymentNotFoundFailure(Cart))
  }
}
