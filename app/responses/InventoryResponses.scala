package responses

import models.inventory._

object InventoryResponses {

  final case class SkuCounts(onHand: Int, onHold: Int, reserved: Int, safetyStock: Option[Int], afs: Int, afsCost: Int)

  object SkuCounts {

    def build(summary: InventorySummary, price: Int): SkuCounts = {
      val afs = summary.availableForSale
      SkuCounts(onHand = summary.onHand, onHold = summary.onHold, reserved = summary.reserved, safetyStock = summary
        .safetyStock, afs = afs, afsCost = afs * price)
    }
  }

  object SkuDetailsResponse {

    final case class Root(skuType: SkuType, counts: SkuCounts)

    def build(summaries: Seq[InventorySummary], warehouse: Warehouse, price: Int): Seq[Root] =
      summaries.map(summary ⇒ Root(skuType = summary.skuType, SkuCounts.build(summary, price)))
  }

  object SkuSummaryResponse {

    final case class Root(warehouse: Warehouse, counts: SkuCounts)

    def build(summary: InventorySummary, warehouse: Warehouse, price: Int): Root =
      Root(warehouse = warehouse, counts = SkuCounts.build(summary, price))
  }
}
