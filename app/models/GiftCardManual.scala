package models

import com.wix.accord.dsl.{validator ⇒ createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class GiftCardManual(id: Int = 0, adminId: Int, reasonId: Int) extends
ModelWithIdParameter

object GiftCardManual {}

class GiftCardManuals(tag: Tag) extends GenericTable.TableWithId[GiftCardManual](tag, "gift_card_manuals")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def reasonId = column[Int]("reason_id")

  def * = (id, adminId, reasonId) <> ((GiftCardManual.apply _).tupled, GiftCardManual.unapply)
}

object GiftCardManuals extends TableQueryWithId[GiftCardManual, GiftCardManuals](
  idLens = GenLens[GiftCardManual](_.id)
  )(new GiftCardManuals(_)){
}
