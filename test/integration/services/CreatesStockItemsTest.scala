package services

import models.StockItems
import util.IntegrationTestBase

class CreatesStockItemsTest extends IntegrationTestBase {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "CreatesStockItems" - {
    "creates a new stock item" in withStockItemSchema {
      val item = CreatesStockItems(productId = 1, onHand = 1, onHold = 0).futureValue
      item mustBe 'right
    }

    "returns error messages on invalid input parameters" in withStockItemSchema {
      val result = CreatesStockItems(productId = 1, onHand = -1, onHold = 0).futureValue
      val error  = result.swap.get

      error must === (List(GeneralFailure("On hand must be >= 0")))
    }
  }

  def withStockItemSchema(testCode: ⇒ Any) = {
    db.run(StockItems.query.schema.create).futureValue
    testCode
    db.run(models.StockItems.query.schema.drop).futureValue
  }
}
