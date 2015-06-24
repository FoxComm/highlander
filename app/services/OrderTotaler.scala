package services

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}


object OrderTotaler {
  def grandTotalForOrder(order: Order)
                        (implicit ec: ExecutionContext, db: Database): Future[Int] = {
    OrderLineItems.findByOrder(order).map{ _.foldLeft(Future(List[Int]()))((sum, lineItem) =>
      db.run(Skus.findById(lineItem.skuId)).map { sku =>
        val skuToSum = sku.getOrElse(Sku(price=0))
        sum. :+ skuToSum.price
      }
    )}
  }
}
