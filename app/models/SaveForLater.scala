package models

import java.time.Instant

import models.customer.Customers
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class SaveForLater(id: Int = 0, customerId: Int = 0, 
  skuId: Int, createdAt: Instant = Instant.now)
  extends FoxModel[SaveForLater] {

}

object SaveForLater {

}

class SaveForLaters(tag: Tag) extends FoxTable[SaveForLater](tag, "save_for_later") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def skuId = column[Int]("sku_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, customerId, skuId, createdAt) <>((SaveForLater.apply _).tupled, SaveForLater.unapply)

  def customer = foreignKey(Customers.tableName, customerId, Customers)(_.id)
}

object SaveForLaters extends FoxTableQuery[SaveForLater, SaveForLaters](new SaveForLaters(_))
  with ReturningId[SaveForLater, SaveForLaters] {

  val returningLens: Lens[SaveForLater, Int] = lens[SaveForLater].id

  def find(customerId: Int, skuId: Int): QuerySeq =
    filter(_.customerId === customerId).filter(_.skuId === skuId)
}
