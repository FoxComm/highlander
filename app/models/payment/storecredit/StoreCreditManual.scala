package models.payment.storecredit

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

case class StoreCreditManual(id: Int = 0, adminId: Int, reasonId: Int, subReasonId: Option[Int] = None) extends
  ModelWithIdParameter[StoreCreditManual]

object StoreCreditManual {}

class StoreCreditManuals(tag: Tag) extends GenericTable.TableWithId[StoreCreditManual](tag, "store_credit_manuals") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def reasonId = column[Int]("reason_id")
  def subReasonId = column[Option[Int]]("sub_reason_id")

  def * = (id, adminId, reasonId, subReasonId) <> ((StoreCreditManual.apply _).tupled, StoreCreditManual.unapply)
}

object StoreCreditManuals extends TableQueryWithId[StoreCreditManual, StoreCreditManuals](
  idLens = GenLens[StoreCreditManual](_.id)
  )(new StoreCreditManuals(_)){
}
