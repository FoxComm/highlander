package failures

object InventoryFailures {

  final case class InventorySummaryNotFound(skuId: Int, warehouseId: Int) extends Failure {
    override def description = s"Summary for sku with id=$skuId in warehouse with id=$warehouseId not found"
  }

}
