package services

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object OrderTotaler {
  def grandTotalForOrder(order: Order)(implicit db: Database): Future[Option[Int]] = {
    db.run((for {
      lineItems <- OrderLineItems._findByOrder(order)
      skus ← Skus if skus.id === lineItems.skuId
    } yield skus).map(_.price).sum.result)
  }
}
