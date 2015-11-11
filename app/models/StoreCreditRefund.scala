package models

import com.wix.accord.dsl.{validator ⇒ createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreCreditRefund(id: Int = 0, rmaId: Int) extends
ModelWithIdParameter[StoreCreditRefund]

object StoreCreditRefund {}

class StoreCreditRefunds(tag: Tag) extends GenericTable.TableWithId[StoreCreditRefund](tag, "store_credit_refunds") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")

  def * = (id, rmaId) <> ((StoreCreditRefund.apply _).tupled, StoreCreditRefund.unapply)
}

object StoreCreditRefunds extends TableQueryWithId[StoreCreditRefund, StoreCreditRefunds](
  idLens = GenLens[StoreCreditRefund](_.id)
)(new StoreCreditRefunds(_)){
}
