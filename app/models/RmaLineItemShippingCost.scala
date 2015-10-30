package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaLineItemShippingCost(id: Int = 0, rmaId: Int, shipmentId: Int)
  extends ModelWithIdParameter[RmaLineItemShippingCost]

object RmaLineItemShippingCost {}

class RmaLineItemShippingCosts(tag: Tag) extends
GenericTable.TableWithId[RmaLineItemShippingCost](tag, "rma_line_item_shipments")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def shipmentId = column[Int]("shipment_id")

  def * = (id, rmaId, shipmentId) <> ((RmaLineItemShippingCost.apply _).tupled, RmaLineItemShippingCost.unapply)
}

object RmaLineItemShippingCosts extends TableQueryWithId[RmaLineItemShippingCost, RmaLineItemShippingCosts](
  idLens = GenLens[RmaLineItemShippingCost](_.id)
)(new RmaLineItemShippingCosts(_)){

}
