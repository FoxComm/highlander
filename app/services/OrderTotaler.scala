package services

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}


object OrderTotaler {
  def grandTotalForOrder(order: Order)
                        (implicit ec: ExecutionContext, db: Database): Future[Int] = {
//    OrderLineItems.findByOrder(order).map { lineItems =>
//      lineItems.map { lineItem =>
//        db.run(Skus.findById(lineItem.skuId)).map { optSku =>
//          optSku.getOrElse(Sku(price=0)).price
//        }
//      }
//    }

//    OrderLineItems.findByOrder(order).map { lineItems =>
//      val ints = lineItems.foldLeft(Future(List[Int]())) { (sum, lineItem) =>
//        sum.flatMap { curSum =>
//          db.run(Skus.findById(lineItem.skuId)).map { sku =>
//            curSum :+ sku.getOrElse(Sku(price=0)).price
//          }
//        }
//      }
//      Future.sequence(ints).map { hi =>
//        hi._1.sum
//      }
//    }

//    for {
//      lineItems <- OrderLineItems.findByOrder(order)
//      sku <- lineItems.map{ lineItem => db.run(Skus.findById(lineItem.skuId)) }
//      summableSku <- sku.map{ nowSku => nowSku.getOrElse(Sku(price = 0)) }
//      prices = summableSku.price
//    } yield (prices)

    Future(3)
  }
}
