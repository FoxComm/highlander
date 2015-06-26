package services

import models._
import payloads.UpdateOrderPayload

import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import slick.driver.PostgresDriver.api._


object OrderUpdater {

  def updateStatus(order: Order, payLoad: UpdateOrderPayload)
                  (implicit db: Database, ec: ExecutionContext): Future[Order Or List[ErrorMessage]]  = {
    val newOrder = order.copy(status = Order.FulfillmentStarted)
    val insertedQuery = for {
      _ <- Orders.insertOrUpdate(newOrder)
      updatedOrder <- Orders.findById(order.id)
    } yield (updatedOrder)
    
    db.run(insertedQuery).map { optOrder =>
      optOrder match {
        case Some(orderExists) => Good(orderExists)
        case None => Bad(List("Not able to update order"))
      }
    }
    
  }
}
