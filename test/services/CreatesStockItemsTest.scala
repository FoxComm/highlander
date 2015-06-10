package services

import models.StockItems
import org.scalatest.{BeforeAndAfter, FreeSpec, MustMatchers, FunSuite}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}

class CreatesStockItemsTest extends FreeSpec
  with util.DbTestSupport
  with MustMatchers
  with ScalaFutures
  with BeforeAndAfter
  with IntegrationPatience {

  import concurrent.ExecutionContext.Implicits.global
  import api._

  "CreatesStockItems" - {
    "creates a new stock item" in withStockItemSchema {
      val item = CreatesStockItems(productId = 1, onHand = 1, onHold = 0).futureValue
      item mustBe 'good
    }

    "returns error messages on invalid input parameters" in withStockItemSchema {
      val result = CreatesStockItems(productId = 1, onHand = -1, onHold = 0).futureValue
      val error  = result.swap.get

      error mustBe "On hand must be >= 0"
    }
  }

  def withStockItemSchema(testCode: ⇒ Any) = {
    db.run(StockItems.query.schema.create).futureValue
    testCode
    db.run(models.StockItems.query.schema.drop).futureValue
  }
}
