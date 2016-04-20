package models.rma

import java.time.Instant

import models.shipping.{Shipment, Shipments}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.db._

case class RmaLineItemShippingCost(id: Int = 0, rmaId: Int, shipmentId: Int, createdAt: Instant = Instant.now)
  extends FoxModel[RmaLineItemShippingCost]

object RmaLineItemShippingCost {}

class RmaLineItemShippingCosts(tag: Tag) extends FoxTable[RmaLineItemShippingCost](tag, "rma_line_item_shipments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def shipmentId = column[Int]("shipment_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, shipmentId,
    createdAt) <> ((RmaLineItemShippingCost.apply _).tupled, RmaLineItemShippingCost.unapply)

  def shipment = foreignKey(Shipments.tableName, shipmentId, Shipments)(_.id)
}

object RmaLineItemShippingCosts extends FoxTableQuery[RmaLineItemShippingCost, RmaLineItemShippingCosts](
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
