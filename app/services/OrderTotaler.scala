package services

import models._
import slick.driver.PostgresDriver.api._
import scala.concurrent.{ExecutionContext, Future}

object OrderTotaler {
  def subTotalForOrder(order: Order): DBIO[Option[Int]] = {
    (for {
      liSku ← OrderLineItemSkus.findByOrderId(order.id)
      sku ← Skus if sku.id === liSku.skuId
    } yield sku).map(_.price).sum.result
  }

  def grandTotalForOrder(order: Order): DBIO[Option[Int]] = {
    (for {
      liSku ← OrderLineItemSkus.findByOrderId(order.id)
      sku ← Skus if sku.id === liSku.skuId
    } yield sku).map(_.price).sum.result
  }
}
