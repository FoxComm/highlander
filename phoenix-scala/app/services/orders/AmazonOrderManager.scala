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
  def findByAmazonOrderId(amazonOrderId: String)(implicit  ec: EC, db: DB, au: AU): DbResultT[AmazonOrderResponse.Root] =
    for {
      amazonOrder <- * <~ AmazonOrders.mustFindOneOr404(amazonOrderId)
    } yield AmazonOrderResponse.build(amazonOrder)

  def createAmazonOrder(payload: CreateAmazonOrderPayload)(implicit  ec: EC, db: DB, au: AU): DbResultT[AmazonOrderResponse.Root] =
    for {
      amazonOrder <- * <~ AmazonOrders.create(AmazonOrder.build(payload))
    } AmazonOrderResponse.build(amazonOrder)

  def listAmazonOrders()(implicit ec: EC, db: DB, ac: AC): DbResultT[ListAmazonOrdersAnswer] = {
    for {
      amazonOrders ← * <~ AmazonOrders.result
    } yield amazonOrders.map(AmazonOrderInfo.fromAmazonOrder)
  }

}
