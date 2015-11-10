package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaLineItemShippingCost(id: Int = 0, rmaId: Int, shipmentId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[RmaLineItemShippingCost]

object RmaLineItemShippingCost {}

class RmaLineItemShippingCosts(tag: Tag) extends
GenericTable.TableWithId[RmaLineItemShippingCost](tag, "rma_line_item_shipments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def shipmentId = column[Int]("shipment_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, shipmentId,
    createdAt) <> ((RmaLineItemShippingCost.apply _).tupled, RmaLineItemShippingCost.unapply)
}

object RmaLineItemShippingCosts extends TableQueryWithId[RmaLineItemShippingCost, RmaLineItemShippingCosts](
  idLens = GenLens[RmaLineItemShippingCost](_.id)
)(new RmaLineItemShippingCosts(_)){

}
