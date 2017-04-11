package services

import cats.implicits._
import payloads.AmazonOrderPayloads._
import models.cord._
import responses.cord.AmazonOrderResponse
import responses.cord.AmazonOrderResponse._
import responses.cord.AmazonOrderResponse.AmazonOrderInfo._
import utils.aliases._
import utils.db._

object AmazonOrderManager {
  def createAmazonOrder(payload: CreateAmazonOrderPayload)(
      implicit ec: EC,
      db: DB,
      au: AU): DbResultT[AmazonOrderResponse.Root] =
    for {
      amazonOrder ← * <~ AmazonOrders.create(AmazonOrder.build(payload))
    } yield AmazonOrderResponse.build(amazonOrder)
}
