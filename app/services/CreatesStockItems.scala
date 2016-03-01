package services

import models.inventory.{StockItem, StockItems}
import utils.aliases._

object CreatesStockItems {
  /** This is just for demonstration purposes! */
  def apply(productId: Int, onHold: Int, onHand: Int)(implicit ec: EC, db: DB): Result[StockItem] = {
    /** We’ll use real validations here soon */
    if (onHand < 0) {
      Result.failure(GeneralFailure(s"On hand must be >= 0"))
    } else {
      val stockItem = StockItem(
        id = 0,
        productId = productId,
        stockLocationId = 0,
        onHold = onHold,
        onHand = onHand,
        allocatedToSales = 0
      )

      Result.fromFuture(db.run(for {
        id ← StockItems.returningId += stockItem
      } yield stockItem.copy(id = id)))
    }
  }
}
