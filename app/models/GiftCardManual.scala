package models

import com.pellucid.sealerate
import services.Failure
import slick.dbio
import slick.dbio.Effect.Write
import utils.Money._
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class GiftCardManual(id: Int = 0, adminId: Int, reasonId: Int) extends
ModelWithIdParameter

object GiftCardManual {}

class GiftCardManuals(tag: Tag) extends GenericTable.TableWithId[GiftCardManual](tag, "gift_card_manuals") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def reasonId = column[Int]("reason_id")

  def * = (id, adminId, reasonId) <> ((GiftCardManual.apply _).tupled, GiftCardManual.unapply)
}

object GiftCardManuals extends TableQueryWithId[GiftCardManual, GiftCardManuals](
  idLens = GenLens[GiftCardManual](_.id)
  )(new GiftCardManuals(_)){
}
