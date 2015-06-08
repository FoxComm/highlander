package services

import models.{StockItems, StockItem}
import org.scalactic.{Good, Bad, ErrorMessage, Or}

import scala.concurrent.{Future, ExecutionContext}

import slick.driver.PostgresDriver.api._

object CreatesStockItems {
  /** This is just for demonstration purposes! */
  def apply(productId: Int, onHold: Int, onHand: Int)
           (implicit ec: ExecutionContext, db: Database): Future[StockItem Or ErrorMessage] = {

    /** We’ll use real validations here soon */
    if (onHand < 0) {
      Future.successful(Bad(s"On hand must be >= 0"))
    } else {
      val stockItem = StockItem(
        id = 0,
        productId = productId,
        stockLocationId = 0,
        onHold = onHold,
        onHand = onHand,
        allocatedToSales = 0
      )

      db.run(for {
        id ← StockItems.returningId += stockItem
      } yield Good(stockItem.copy(id = id)))
    }
  }
}
