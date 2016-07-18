package models.cord

import failures.CartFailures.OrderAlreadyPlaced
import failures.{Failure, NotFoundFailure404}
import models.customer.Customer
import models.traits.Lockable
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.Money.Currency
import utils.aliases._
import utils.db._

case class Cart(id: Int = 0,
                referenceNumber: String = "",
                customerId: Int,
                currency: Currency = Currency.USD,
                subTotal: Int = 0,
                shippingTotal: Int = 0,
                adjustmentsTotal: Int = 0,
                taxesTotal: Int = 0,
                grandTotal: Int = 0,
                // Cart-specific
                isLocked: Boolean = false)
    extends CordBase[Cart]
    with Lockable[Cart] {

  override def primarySearchKey: String = referenceNumber

  def toOrder()(implicit ec: EC, ctx: OC): Order =
    Order(referenceNumber = referenceNumber,
          customerId = customerId,
          currency = currency,
          subTotal = subTotal,
          shippingTotal = shippingTotal,
          adjustmentsTotal = adjustmentsTotal,
          taxesTotal = taxesTotal,
          grandTotal = grandTotal,
          state = Order.RemorseHold,
          contextId = ctx.id)

  // TODO: remove this in favor of implicit param
  def toOrder(contextId: Int)(implicit ec: EC): Order =
    Order(referenceNumber = referenceNumber,
          customerId = customerId,
          currency = currency,
          subTotal = subTotal,
          shippingTotal = shippingTotal,
          adjustmentsTotal = adjustmentsTotal,
          taxesTotal = taxesTotal,
          grandTotal = grandTotal,
          contextId = contextId)
}

class Carts(tag: Tag) extends FoxTable[Cart](tag, "carts") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber  = column[String]("reference_number")
  def customerId       = column[Int]("customer_id")
  def currency         = column[Currency]("currency")
  def subTotal         = column[Int]("sub_total")
  def shippingTotal    = column[Int]("shipping_total")
  def adjustmentsTotal = column[Int]("adjustments_total")
  def taxesTotal       = column[Int]("taxes_total")
  def grandTotal       = column[Int]("grand_total")
  def isLocked         = column[Boolean]("is_locked")

  def * =
    (id,
     referenceNumber,
     customerId,
     currency,
     subTotal,
     shippingTotal,
     adjustmentsTotal,
     taxesTotal,
     grandTotal,
     isLocked) <> ((Cart.apply _).tupled, Cart.unapply)
}

object Carts
    extends FoxTableQuery[Cart, Carts](new Carts(_))
    with ReturningIdAndString[Cart, Carts]
    with SearchByRefNum[Cart, Carts] {

  def findByCustomer(cust: Customer): QuerySeq =
    findByCustomerId(cust.id)

  def findByCustomerId(customerId: Int): QuerySeq =
    filter(_.customerId === customerId)

  def findByRefNum(refNum: String): QuerySeq =
    filter(_.referenceNumber === refNum)

  def findOneByRefNum(refNum: String): DBIO[Option[Cart]] =
    filter(_.referenceNumber === refNum).one

  def findByRefNumAndCustomer(refNum: String, customer: Customer): QuerySeq =
    filter(_.referenceNumber === refNum).filter(_.customerId === customer.id)

  override def mustFindByRefNum(refNum: String, notFoundFailure: String ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[Cart] =
    for {
      cord ← * <~ Cords.mustFindByRefNum(refNum, _ ⇒ NotFoundFailure404(Cart, refNum))
      cart ← * <~ (if (cord.isCart) super.mustFindByRefNum(refNum)
                   else DbResultT.failure(OrderAlreadyPlaced(refNum)))
    } yield cart

  private val rootLens = lens[Cart]

  val returningLens: Lens[Cart, (Int, String)] = rootLens.id ~ rootLens.referenceNumber
  override val returningQuery = map { c ⇒
    (c.id, c.referenceNumber)
  }
}
