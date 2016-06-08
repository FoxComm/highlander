package failures

object InventoryFailures {

  case class InventorySummaryNotFound(skuId: Int, warehouseId: Int) extends Failure {
    override def description =
      s"Summary for sku with id=$skuId in warehouse with id=$warehouseId not found"
  }

  case class NotEnoughItems(skuCode: String, available: Int, required: Int) extends Failure {
    override def description =
      s"Not enough items for SKU $skuCode: available=$available, required=$required"
  }
}
