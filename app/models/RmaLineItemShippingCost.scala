package models

import java.time.Instant

import models.RmaLineItemSkus._
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

  def shipment = foreignKey(Shipments.tableName, shipmentId, Shipments)(_.id)
}

object RmaLineItemShippingCosts extends TableQueryWithId[RmaLineItemShippingCost, RmaLineItemShippingCosts](
  idLens = GenLens[RmaLineItemShippingCost](_.id)
)(new RmaLineItemShippingCosts(_)){

  def findByRmaId(rmaId: Rep[Int]): QuerySeq =
    filter(_.rmaId === rmaId)

  def findLineItemsByRma(rma: Rma): Query[(Shipments, RmaLineItems), (Shipment, RmaLineItem), Seq] = for {
    liSc ← findByRmaId(rma.id)
    li ← RmaLineItems if li.originId === liSc.id
    shipment ← Shipments if shipment.id === liSc.shipmentId
  } yield (shipment, li)
}
