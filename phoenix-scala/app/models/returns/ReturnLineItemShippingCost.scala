package models.returns

import java.time.Instant
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class ReturnLineItemShippingCost(id: Int = 0,
                                      returnId: Int,
                                      amount: Int,
                                      createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItemShippingCost]

object ReturnLineItemShippingCost {}

class ReturnLineItemShippingCosts(tag: Tag)
    extends FoxTable[ReturnLineItemShippingCost](tag, "return_line_item_shipping_costs") {
  def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def returnId  = column[Int]("return_id")
  def amount    = column[Int]("amount")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, returnId, amount, createdAt) <> ((ReturnLineItemShippingCost.apply _).tupled, ReturnLineItemShippingCost.unapply)

  def returns = foreignKey(Returns.tableName, returnId, Returns)(_.id)
}

object ReturnLineItemShippingCosts
    extends FoxTableQuery[ReturnLineItemShippingCost, ReturnLineItemShippingCosts](
        new ReturnLineItemShippingCosts(_))
    with ReturningId[ReturnLineItemShippingCost, ReturnLineItemShippingCosts] {

  val returningLens: Lens[ReturnLineItemShippingCost, Int] = lens[ReturnLineItemShippingCost].id

  def findByRmaId(returnId: Rep[Int]): QuerySeq =
    filter(_.returnId === returnId)

  def findLineItemByRma(rma: Return)(
      implicit ec: EC): DbResultT[Option[(ReturnLineItemShippingCost, ReturnLineItem)]] =
    findByRmaId(rma.id).join(ReturnLineItems).on(_.id === _.originId).one.dbresult

  def findByOrderRef(orderRef: String)(implicit ec: EC): QuerySeq =
    Returns
      .findByOrderRefNum(orderRef)
      .join(ReturnLineItemShippingCosts)
      .on(_.id === _.returnId)
      .map { case (_, shippingCost) ⇒ shippingCost }
}
