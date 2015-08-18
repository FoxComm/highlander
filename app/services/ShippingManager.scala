package services

import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

import models.Order

object ShippingManager {
  def getMatchingShippingMethods(order: Order)(implicit db: Database, ec: ExecutionContext) = {
    db.run(models.Conditions.result).map { conditions ⇒
      conditions.map { condition ⇒
        condition.subject match {
          case "Order" ⇒
            condition.field match {
              case "subTotal" ⇒
                // Check the subtotal.
              case "total" ⇒
                // Check the total.
              case _ ⇒
                // Whoops, this is invalid.
            }
          case _ ⇒ // We don't support this.
        }
      }
    }
  }
}