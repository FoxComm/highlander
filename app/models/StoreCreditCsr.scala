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

final case class StoreCreditCsr(id: Int = 0, adminId: Int, reason: String, subReason: Option[String] = None) extends
ModelWithIdParameter

object StoreCreditCsr {}

class StoreCreditCsrs(tag: Tag) extends GenericTable.TableWithId[StoreCreditCsr](tag, "store_credit_csrs")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId = column[Int]("admin_id")
  def reason = column[String]("reason")
  def subReason = column[Option[String]]("sub_reason")

  def * = (id, adminId, reason, subReason) <> ((StoreCreditCsr.apply _).tupled, StoreCreditCsr.unapply)
}

object StoreCreditCsrs extends TableQueryWithId[StoreCreditCsr, StoreCreditCsrs](
  idLens = GenLens[StoreCreditCsr](_.id)
  )(new StoreCreditCsrs(_)){
}
