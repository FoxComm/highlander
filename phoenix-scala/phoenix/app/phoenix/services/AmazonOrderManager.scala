package phoenix.services

import cats.implicits._
import phoenix.payloads.AmazonOrderPayloads._
import phoenix.models.cord._
import phoenix.models.account._
import phoenix.responses.cord.AmazonOrderResponse
import phoenix.utils.aliases._
import core.db._
import core.failures._

object AmazonOrderManager {

  def createAmazonOrder(
      payload: CreateAmazonOrderPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[AmazonOrderResponse] =
    for {
      user ← * <~ Users
              .findByEmail(payload.customerEmail)
              .mustFindOneOr(NotFoundFailure404(User, "email", payload.customerEmail))
      inner = AmazonOrders.create(AmazonOrder.build(payload, user.accountId))
      amazonOrder ← * <~ AmazonOrders
                     .findOneByAmazonOrderId(payload.amazonOrderId)
                     .findOrCreate(inner)
    } yield AmazonOrderResponse.build(amazonOrder)

  def updateAmazonOrder(
      amazonOrderId: String,
      payload: UpdateAmazonOrderPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[AmazonOrderResponse] =
    for {
      amazonOrder ← * <~ AmazonOrders.mustFindOneOr(amazonOrderId)
      up          ← * <~ AmazonOrders.update(amazonOrder, amazonOrder.copy(orderStatus = payload.orderStatus))
    } yield AmazonOrderResponse.build(up)
}
