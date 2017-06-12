package phoenix.models.payment.storecredit

import shapeless._
import slick.jdbc.PostgresProfile.api._
import core.db._

case class StoreCreditRefund(id: Int = 0, returnId: Int) extends FoxModel[StoreCreditRefund]

object StoreCreditRefund {}

class StoreCreditRefunds(tag: Tag) extends FoxTable[StoreCreditRefund](tag, "store_credit_refunds") {
  def id       = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId = column[Int]("return_id")

  def * = (id, returnId) <> ((StoreCreditRefund.apply _).tupled, StoreCreditRefund.unapply)
}

object StoreCreditRefunds
    extends FoxTableQuery[StoreCreditRefund, StoreCreditRefunds](new StoreCreditRefunds(_))
    with ReturningId[StoreCreditRefund, StoreCreditRefunds] {

  val returningLens: Lens[StoreCreditRefund, Int] = lens[StoreCreditRefund].id
}
