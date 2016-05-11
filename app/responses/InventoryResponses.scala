package responses

import models.inventory._
import models.inventory.summary.InventorySummary.AllSummaries
import models.inventory.summary._

object InventoryResponses {

  case class SkuCounts(onHand: Int, onHold: Int, reserved: Int, safetyStock: Option[Int], afs: Int, afsCost: Int)

  object SkuCounts {

    def build[A <: InventorySummaryBase[A]](summary: A, price: Int): SkuCounts = summary match {
      case sellable: SellableInventorySummary ⇒
        SkuCounts(onHand = sellable.onHand, onHold = sellable.onHold, reserved = sellable.reserved, safetyStock =
          Some(sellable.safetyStock), afs = sellable.availableForSale, afsCost = sellable.availableForSaleCost(price))
      case _ ⇒
        SkuCounts(onHand = summary.onHand, onHold = summary.onHold, reserved = summary.reserved, safetyStock =
          None, afs = summary.availableForSale, afsCost = summary.availableForSaleCost(price))
    }
  }

  object SkuDetailsResponse {

    case class Root(skuType: SkuType, counts: SkuCounts)

    def build(summaries: AllSummaries, price: Int): Seq[Root] = Seq(
      Root(Sellable, SkuCounts.build(summaries._1, price)),
      Root(Backorder, SkuCounts.build(summaries._2, price)),
      Root(Preorder, SkuCounts.build(summaries._3, price)),
      Root(NonSellable, SkuCounts.build(summaries._4, price))
    )
  }

  object SellableSkuSummaryResponse {

    case class Root(warehouse: Warehouse, counts: SkuCounts)

    def build(summary: SellableInventorySummary, warehouse: Warehouse, price: Int): Root =
      Root(warehouse = warehouse, counts = SkuCounts.build(summary, price))
  }

  object WmsOverrideResponse {

    case class Root(updatedValuesCount: Int)

    def build(updatedValuesCount: Int): Root = Root(updatedValuesCount)
  }
}
