package services

import models._

import slick.driver.PostgresDriver.api._
import scala.concurrent.{ExecutionContext, Future}

object OrderTotaler {
  def subTotalForOrder(order: Order)(implicit db: Database, ec: ExecutionContext): Future[Int] = {
    db.run((for {
      lineItems <- OrderLineItems._findByOrder(order)
      skus ← Skus if skus.id === lineItems.skuId
    } yield skus).map(_.price).sum.result).map(_.getOrElse(0))
  }
}
