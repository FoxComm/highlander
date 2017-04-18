package services

import cats.implicits._
import payloads.AmazonOrderPayloads._
import models.cord._
import responses.cord.AmazonOrderResponse
import utils.aliases._
import utils.db._

object AmazonOrderManager {
  def createAmazonOrder(payload: CreateAmazonOrderPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[AmazonOrderResponse.Root] = {

    val amazonOrderT = for {
      result ← * <~ AmazonOrders
                .findOneByAmazonOrderId(payload.amazonOrderId)
                .findOrCreateExtended(AmazonOrders.create(AmazonOrder.build(payload)))
      (existingOrder, foundOrCreated) = result
    } yield result

    amazonOrderT.map {
      case (existingOrder, foundOrCreated) ⇒
        AmazonOrderResponse.build(AmazonOrder.fromExistingAmazonOrder(existingOrder))
    }
  }
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
