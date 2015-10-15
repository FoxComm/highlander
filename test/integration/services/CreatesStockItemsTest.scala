package services

import cats.data.Xor
import models.StockItems
import org.scalatest.Inside
import util.IntegrationTestBase

class CreatesStockItemsTest extends IntegrationTestBase with Inside {
  import api._
  import concurrent.ExecutionContext.Implicits.global

  "CreatesStockItems" - {
    "creates a new stock item" in withStockItemSchema {
      val item = CreatesStockItems(productId = 1, onHand = 1, onHold = 0).futureValue
      item mustBe 'right
    }

    "returns error messages on invalid input parameters" in withStockItemSchema {
      val result = CreatesStockItems(productId = 1, onHand = -1, onHold = 0).futureValue
      result mustBe 'left

      inside(result) {
        case Xor.Left(nel) ⇒
          inside(nel.head) {
            case GeneralFailure(message) ⇒
              message must include ("On hand must be >= 0")
          }
      }
    }
  }

  def withStockItemSchema(testCode: ⇒ Any) = {
    db.run(StockItems.query.schema.create).futureValue
    testCode
    db.run(models.StockItems.query.schema.drop).futureValue
  }
}
