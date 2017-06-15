package phoenix.models.payment.storecredit

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._

case class StoreCreditManual(id: Int = 0, adminId: Int, reasonId: Int, subReasonId: Option[Int] = None)
    extends FoxModel[StoreCreditManual]

object StoreCreditManual {}

class StoreCreditManuals(tag: Tag) extends FoxTable[StoreCreditManual](tag, "store_credit_manuals") {
  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId     = column[Int]("admin_id")
  def reasonId    = column[Int]("reason_id")
  def subReasonId = column[Option[Int]]("sub_reason_id")

  def * =
    (id, adminId, reasonId, subReasonId) <> ((StoreCreditManual.apply _).tupled, StoreCreditManual.unapply)
}

object StoreCreditManuals
    extends FoxTableQuery[StoreCreditManual, StoreCreditManuals](new StoreCreditManuals(_))
    with ReturningId[StoreCreditManual, StoreCreditManuals] {

  val returningLens: Lens[StoreCreditManual, Int] = lens[StoreCreditManual].id
}
