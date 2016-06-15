package models

import scala.concurrent.ExecutionContext.Implicits.global

import models.inventory.InventoryAdjustment._
import models.inventory._
import org.json4s.JsonAST.JString
import util.IntegrationTestBase
import utils.db.DbResultT._

class InventoryAdjustmentIntegrationTest extends IntegrationTestBase {

  "Inventory adjustment model" - {

    "allows safety stock adjustment only for sellables" in {
      def adjustment(skuType: SkuType) =
        InventoryAdjustment(summaryId = 1,
                            change = 1,
                            skuType = skuType,
                            state = SafetyStock,
                            metadata = JString("foo"),
                            newQuantity = 1,
                            newAfs = 1)

      InventoryAdjustments.create(adjustment(Sellable)).run().futureValue mustBe 'right
      InventoryAdjustments.create(adjustment(Preorder)).run().futureValue mustBe 'left
      InventoryAdjustments.create(adjustment(Backorder)).run().futureValue mustBe 'left
      InventoryAdjustments.create(adjustment(NonSellable)).run().futureValue mustBe 'left
    }
  }
}
