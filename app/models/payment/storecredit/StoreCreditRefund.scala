package models.payment.storecredit

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class StoreCreditRefund(id: Int = 0, rmaId: Int) extends FoxModel[StoreCreditRefund]

object StoreCreditRefund {}

class StoreCreditRefunds(tag: Tag) extends FoxTable[StoreCreditRefund](tag, "store_credit_refunds") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")

  def * = (id, rmaId) <> ((StoreCreditRefund.apply _).tupled, StoreCreditRefund.unapply)
}

object StoreCreditRefunds extends FoxTableQuery[StoreCreditRefund, StoreCreditRefunds](new StoreCreditRefunds(_))
  with ReturningId[StoreCreditRefund, StoreCreditRefunds] {

  val returningLens: Lens[StoreCreditRefund, Int] = lens[StoreCreditRefund].id
}
