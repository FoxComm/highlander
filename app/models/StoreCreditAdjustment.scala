package models

import utils.{GenericTable, TableQueryWithId, ModelWithIdParameter, RichTable}

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

final case class StoreCreditAdjustment(id: Int = 0, storeCreditId: Int, debit: Int, capture: Boolean)
  extends ModelWithIdParameter

object StoreCreditAdjustment {}

class StoreCreditAdjustments(tag: Tag)
  extends GenericTable.TableWithId[StoreCreditAdjustment](tag, "store_credit_adjustments")
  with RichTable {

  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeCreditId = column[Int]("store_credit_id")
  def debit = column[Int]("debit")
  def capture = column[Boolean]("capture")

  def * = (id, storeCreditId, debit, capture) <> ((StoreCreditAdjustment.apply _).tupled,
    StoreCreditAdjustment.unapply)
}

object StoreCreditAdjustments
  extends TableQueryWithId[StoreCreditAdjustment, StoreCreditAdjustments](
  idLens = GenLens[StoreCreditAdjustment](_.id)
  )(new StoreCreditAdjustments(_)){
}

