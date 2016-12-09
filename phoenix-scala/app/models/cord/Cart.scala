package models.cord

import com.github.tminglei.slickpg.LTree
import failures.CartFailures.OrderAlreadyPlaced
import failures.{Failure, NotFoundFailure404}
import models.account.Account
import models.traits.Lockable
import shapeless._
import utils.db.ExPostgresDriver.api._
import utils.Money.Currency
import utils.aliases._
import utils.db._

case class Cart(id: Int = 0,
                scope: LTree,
                referenceNumber: String = "",
                accountId: Int,
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
}

class Carts(tag: Tag) extends FoxTable[Cart](tag, "carts") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope            = column[LTree]("scope")
  def referenceNumber  = column[String]("reference_number")
  def accountId        = column[Int]("account_id")
  def currency         = column[Currency]("currency")
  def subTotal         = column[Int]("sub_total")
  def shippingTotal    = column[Int]("shipping_total")
  def adjustmentsTotal = column[Int]("adjustments_total")
  def taxesTotal       = column[Int]("taxes_total")
  def grandTotal       = column[Int]("grand_total")
  def isLocked         = column[Boolean]("is_locked")

  def * =
    (id,
     scope,
     referenceNumber,
     accountId,
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

  def findByAccount(cust: Account): QuerySeq =
    findByAccountId(cust.id)

  def findByAccountId(accountId: Int): QuerySeq =
    filter(_.accountId === accountId)

  def findByRefNum(refNum: String): QuerySeq =
    filter(_.referenceNumber === refNum)

  def findOneByRefNum(refNum: String): DBIO[Option[Cart]] =
    filter(_.referenceNumber === refNum).one

  def findByRefNumAndAccountId(refNum: String, accountId: Int): QuerySeq =
    filter(_.referenceNumber === refNum).filter(_.accountId === accountId)

  override def mustFindByRefNum(refNum: String, notFoundFailure: String ⇒ Failure = notFound404K)(
      implicit ec: EC,
      db: DB): DbResultT[Cart] =
    for {
      cord ← * <~ Cords.mustFindByRefNum(refNum, _ ⇒ NotFoundFailure404(Cart, refNum))
      cart ← * <~ doOrFail(cord.isCart, super.mustFindByRefNum(refNum), OrderAlreadyPlaced(refNum))
    } yield cart

  private val rootLens = lens[Cart]

  val returningLens: Lens[Cart, (Int, String)] = rootLens.id ~ rootLens.referenceNumber
  override val returningQuery = map { c ⇒
    (c.id, c.referenceNumber)
  }
}
