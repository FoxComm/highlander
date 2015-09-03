package services

import models._

import slick.driver.PostgresDriver.api._
import scala.concurrent.{ExecutionContext, Future}

object OrderTotaler {
  def _subTotalForOrder(order: Order): DBIO[Option[Int]] = {
    (for {
      lineItems ← OrderLineItems._findByOrder(order)
      skus ← Skus if skus.id === lineItems.skuId
    } yield skus).map(_.price).sum.result
  }

  def subTotalForOrder(order: Order)(implicit db: Database, ec: ExecutionContext): Future[Int] = {
    db.run(_subTotalForOrder(order)).map(_.getOrElse(0))
  }

  def _grandTotalForOrder(order: Order): DBIO[Option[Int]] = {
    (for {
      lineItems ← OrderLineItems._findByOrder(order)
      skus ← Skus if skus.id === lineItems.skuId
    } yield skus).map(_.price).sum.result
  }

  def grandTotalForOrder(order: Order)(implicit db: Database, ec: ExecutionContext): Future[Int] = {
    db.run(_grandTotalForOrder(order)).map(_.getOrElse(0))
  }
}
