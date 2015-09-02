package models

import com.wix.accord.dsl.{validator ⇒ createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, RichTable, TableQueryWithId}

final case class StoreCreditManual(id: Int = 0, adminId: Int, reasonId: Int) extends
ModelWithIdParameter

object StoreCreditManual {}

class StoreCreditManuals(tag: Tag) extends GenericTable.TableWithId[StoreCreditManual](tag, "store_credit_manuals")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def reasonId = column[Int]("reason_id")

  def * = (id, adminId, reasonId) <> ((StoreCreditManual.apply _).tupled, StoreCreditManual.unapply)
}

object StoreCreditManuals extends TableQueryWithId[StoreCreditManual, StoreCreditManuals](
  idLens = GenLens[StoreCreditManual](_.id)
  )(new StoreCreditManuals(_)){
}
