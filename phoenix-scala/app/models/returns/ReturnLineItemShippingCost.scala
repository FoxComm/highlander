package models.returns

import java.time.Instant
import models.shipping.{Shipment, Shipments}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class ReturnLineItemShippingCost(id: Int = 0,
                                      returnId: Int,
                                      shipmentId: Int,
                                      amount: Int,
                                      createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItemShippingCost]

object ReturnLineItemShippingCost {}

class ReturnLineItemShippingCosts(tag: Tag)
    extends FoxTable[ReturnLineItemShippingCost](tag, "return_line_item_shipments") {
  def id         = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId   = column[Int]("return_id")
  def shipmentId = column[Int]("shipment_id")
  def amount     = column[Int]("amount")
  def createdAt  = column[Instant]("created_at")

  def * =
    (id, returnId, shipmentId, amount, createdAt) <> ((ReturnLineItemShippingCost.apply _).tupled, ReturnLineItemShippingCost.unapply)

  def shipment = foreignKey(Shipments.tableName, shipmentId, Shipments)(_.id)
}

object ReturnLineItemShippingCosts
    extends FoxTableQuery[ReturnLineItemShippingCost, ReturnLineItemShippingCosts](
        new ReturnLineItemShippingCosts(_))
    with ReturningId[ReturnLineItemShippingCost, ReturnLineItemShippingCosts] {

  val returningLens: Lens[ReturnLineItemShippingCost, Int] = lens[ReturnLineItemShippingCost].id

  def findByRmaId(returnId: Rep[Int]): QuerySeq =
    filter(_.returnId === returnId)

  def findLineItemsByRma(
      rma: Return): Query[(Shipments, ReturnLineItems), (Shipment, ReturnLineItem), Seq] =
    for {
      liSc     ← findByRmaId(rma.id)
      li       ← ReturnLineItems if li.originId === liSc.id
      shipment ← Shipments if shipment.id === liSc.shipmentId
    } yield (shipment, li)
}
