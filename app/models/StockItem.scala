package models

import slick.driver.PostgresDriver.api._

final case class StockItem(id: Int, productId: Int, stockLocationId: Int, onHold: Int, onHand: Int, allocatedToSales: Int) {
  def available: Int = {
    this.onHand - this.onHold - this.allocatedToSales
  }
}

class StockItems(tag: Tag) extends Table[models.StockItem](tag, "stock_items") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def productId        = column[Int]("product_id")
  def stockLocationId  = column[Int]("stock_location_id")
  def onHold           = column[Int]("on_hold")
  def onHand           = column[Int]("on_hand")
  def allocatedToSales = column[Int]("allocated_to_sales")

  def * = (
    id, productId, stockLocationId, onHold, onHand, allocatedToSales
    ) <> ((models.StockItem.apply _).tupled, models.StockItem.unapply)
}

object StockItems {
  val query       = TableQuery[StockItems]
  val returningId = query.returning(query.map(_.id))
  val findById    = query.findBy(_.id)
}
