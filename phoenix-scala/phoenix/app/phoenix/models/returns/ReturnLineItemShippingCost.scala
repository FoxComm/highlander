package phoenix.models.returns

import java.time.Instant

import core.db._
import shapeless._
import slick.jdbc.PostgresProfile.api._
import core.utils.Money._

case class ReturnLineItemShippingCost(id: Int, returnId: Int, amount: Long, createdAt: Instant = Instant.now)
    extends FoxModel[ReturnLineItemShippingCost]

class ReturnLineItemShippingCosts(tag: Tag)
    extends FoxTable[ReturnLineItemShippingCost](tag, "return_line_item_shipping_costs") {
  def id        = column[Int]("id", O.PrimaryKey)
  def returnId  = column[Int]("return_id")
  def amount    = column[Long]("amount")
  def createdAt = column[Instant]("created_at")

  def * =
    (id, returnId, amount, createdAt) <> ((ReturnLineItemShippingCost.apply _).tupled, ReturnLineItemShippingCost.unapply)

  def li =
    foreignKey(ReturnLineItems.tableName, id, ReturnLineItems)(_.id, onDelete = ForeignKeyAction.Cascade)
  def returns = foreignKey(Returns.tableName, returnId, Returns)(_.id)
}

object ReturnLineItemShippingCosts
    extends FoxTableQuery[ReturnLineItemShippingCost, ReturnLineItemShippingCosts](
      new ReturnLineItemShippingCosts(_))
    with ReturningId[ReturnLineItemShippingCost, ReturnLineItemShippingCosts] {

  val returningLens: Lens[ReturnLineItemShippingCost, Int] = lens[ReturnLineItemShippingCost].id

  def findByRmaId(returnId: Int): QuerySeq =
    filter(_.returnId === returnId)
}
