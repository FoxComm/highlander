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
//    for {
//      amazonOrder ← * <~ AmazonOrders.create(AmazonOrder.build(payload))
//    } yield AmazonOrderResponse.build(amazonOrder)
}
