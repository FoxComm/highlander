package services

import cats.implicits._
import payloads.AmazonOrderPayloads._
import models.cord._
import models.account._
import responses.cord.AmazonOrderResponse
import utils.aliases._
import utils.db._
import failures._

object AmazonOrderManager {

  def createAmazonOrder(payload: CreateAmazonOrderPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[AmazonOrderResponse.Root] = {

    val amazonOrderT =
      AmazonOrders.findOneByAmazonOrderId(payload.amazonOrderId).findOrCreate(createInner(payload))

    amazonOrderT.map {
      case (existingOrder) ⇒
        AmazonOrderResponse.build(AmazonOrder.fromExistingAmazonOrder(existingOrder))
    }
  }

  private def createInner(
      payload: CreateAmazonOrderPayload)(implicit ec: EC, db: DB, au: AU): DbResultT[AmazonOrder] =
    for {
      user ← * <~ Users
              .findByEmail(payload.customerEmail)
              .mustFindOneOr(NotFoundFailure404(User, payload.customerEmail))
      amazonOrder ← * <~ AmazonOrders.create(AmazonOrder.build(payload, user.accountId))
    } yield amazonOrder

  def updateAmazonOrder(amazonOrderId: String, payload: UpdateAmazonOrderPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[AmazonOrderResponse.Root] =
    for {
      amazonOrder ← * <~ AmazonOrders.mustFindOneOr404(amazonOrderId)
      up ← * <~ AmazonOrders.update(amazonOrder,
                                    amazonOrder.copy(orderStatus = payload.orderStatus))
    } yield AmazonOrderResponse.build(up)
}
