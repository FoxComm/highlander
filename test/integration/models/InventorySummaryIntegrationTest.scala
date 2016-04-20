package models

import models.inventory.summary._
import util.IntegrationTestBase
import utils.db._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.db.DbResultT._

import scala.concurrent.ExecutionContext.Implicits.global

class InventorySummaryIntegrationTest extends IntegrationTestBase {

  "Trigger calculates AFS" - {

    "for sellable summary" in {
      val unsaved = SellableInventorySummary(onHand = 10, onHold = 1, reserved = 2, safetyStock = 3)
      val summary = SellableInventorySummaries.create(unsaved).run().futureValue.rightVal
      summary.availableForSale must === (4)
      SellableInventorySummaries.update(summary, summary.copy(safetyStock = 5)).run().futureValue.rightVal
      // FIXME https://github.com/FoxComm/phoenix-scala/issues/821
      val updated = SellableInventorySummaries.findOneById(summary.id).run().futureValue.value
      updated.onHand must === (10)
      updated.onHold must === (1)
      updated.reserved must === (2)
      updated.safetyStock must === (5)
      updated.availableForSale must === (2)
    }

    "for preorder summary" in {
      val unsaved = PreorderInventorySummary(onHand = 10, onHold = 1, reserved = 2)
      val summary = PreorderInventorySummaries.create(unsaved).run().futureValue.rightVal
      summary.availableForSale must === (7)
      PreorderInventorySummaries.update(summary, summary.copy(reserved = 5)).run().futureValue.rightVal
      // FIXME https://github.com/FoxComm/phoenix-scala/issues/821
      val updated = PreorderInventorySummaries.findOneById(summary.id).run().futureValue.value
      updated.onHand must === (10)
      updated.onHold must === (1)
      updated.reserved must === (5)
      updated.availableForSale must === (4)
    }

    "for backorder summary" in {
      val unsaved = BackorderInventorySummary(onHand = 10, onHold = 1, reserved = 2)
      val summary = BackorderInventorySummaries.create(unsaved).run().futureValue.rightVal
      summary.availableForSale must === (7)
      BackorderInventorySummaries.update(summary, summary.copy(onHold = 0)).run().futureValue.rightVal
      // FIXME https://github.com/FoxComm/phoenix-scala/issues/821
      val updated = BackorderInventorySummaries.findOneById(summary.id).run().futureValue.value
      updated.onHand must === (10)
      updated.onHold must === (0)
      updated.reserved must === (2)
      updated.availableForSale must === (8)
    }

    "for nonsellable summary" in {
      val unsaved = NonSellableInventorySummary(onHand = 10, onHold = 1, reserved = 2)
      val summary = NonSellableInventorySummaries.create(unsaved).run().futureValue.rightVal
      summary.availableForSale must === (7)
      NonSellableInventorySummaries.update(summary, summary.copy(onHand = 11)).run().futureValue.rightVal
      // FIXME https://github.com/FoxComm/phoenix-scala/issues/821
      val updated = NonSellableInventorySummaries.findOneById(summary.id).run().futureValue.value
      updated.onHand must === (11)
      updated.onHold must === (1)
      updated.reserved must === (2)
      updated.availableForSale must === (8)
    }
  }
}
