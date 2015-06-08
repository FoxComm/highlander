package services

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
  implicit val database: api.Database = db

  before {
    db.run(tables.StockItems.query.schema.create).futureValue
  }

   after {
    db.run(tables.StockItems.query.schema.drop).futureValue
  }

  "CreatesStockItems" - {
    "creates a new stock item" in {
      val item = CreatesStockItems(productId = 1, onHand = 1, onHold = 0).futureValue
      item mustBe 'good
    }

    "returns error messages on invalid input parameters" in {
      val result = CreatesStockItems(productId = 1, onHand = -1, onHold = 0).futureValue
      val error  = result.swap.get

      error mustBe "On hand must be >= 0"
    }
  }
}
