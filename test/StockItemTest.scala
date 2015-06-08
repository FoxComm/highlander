import models.StockItem
import org.scalatest.{MustMatchers, FreeSpec}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import util.DbTestSupport

/** This is just for demonstration purposes. */
class StockItemTest extends FreeSpec
 with MustMatchers
 with ScalaFutures
 with IntegrationPatience
 with DbTestSupport {

  import api._
  import tables._

  import concurrent.ExecutionContext.Implicits.global

  "Stock Item" - {
    "can be created and queried" in {
      val newStockItem = StockItem(
        id = 0,
        productId        = 1000,
        stockLocationId  = 20,
        onHold           = 0,
        onHand           = 1,
        allocatedToSales = 0
      )

      val roundTripAction = for {
        _    ← StockItems.query.schema.create
        id   ← StockItems.returningId += newStockItem
        item ← StockItems.findById(id).result.head
      } yield item

      val persisted = db.run(roundTripAction.transactionally).futureValue

      persisted.productId        mustBe 1000
      persisted.stockLocationId  mustBe 20
      persisted.onHold           mustBe 0
      persisted.onHand           mustBe 1
      persisted.allocatedToSales mustBe 0
    }
  }
}
