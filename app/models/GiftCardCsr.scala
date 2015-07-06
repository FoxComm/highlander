package models

import com.pellucid.sealerate
import services.Failure
import slick.dbio
import slick.dbio.Effect.Write
import utils.Money._
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import validators.nonEmptyIf

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class GiftCardCsr(id: Int = 0, adminId: Int, reason: String, subReason: Option[String]) extends
ModelWithIdParameter

object GiftCardCsr {}

class GiftCardCsrs(tag: Tag) extends GenericTable.TableWithId[GiftCardCsr](tag, "gift_card_csrs") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def reason = column[String]("reason")
  def subReason = column[Option[String]]("sub_reason")

  def * = (id, adminId, reason, subReason) <> ((GiftCardCsr.apply _).tupled, GiftCardCsr.unapply)
}

object GiftCardCsrs extends TableQueryWithId[GiftCardCsr, GiftCardCsrs](
  idLens = GenLens[GiftCardCsr](_.id)
  )(new GiftCardCsrs(_)){
}
