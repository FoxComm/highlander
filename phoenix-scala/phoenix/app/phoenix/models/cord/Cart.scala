package phoenix.models.cord

import com.github.tminglei.slickpg.LTree
import core.failures.{Failure, NotFoundFailure404}
import phoenix.failures.CartFailures.OrderAlreadyPlaced
import phoenix.models.account.Account
import shapeless._
import core.utils.Money.Currency
import core.db.ExPostgresDriver.api._
import core.db._

case class Cart(id: Int = 0,
                scope: LTree,
                referenceNumber: String = "",
                accountId: Int,
                currency: Currency = Currency.USD,
                subTotal: Long = 0,
                shippingTotal: Long = 0,
                adjustmentsTotal: Long = 0,
                taxesTotal: Long = 0,
                grandTotal: Long = 0)
    extends CordBase[Cart] {

  override def primarySearchKey: String = referenceNumber
}

class Carts(tag: Tag) extends FoxTable[Cart](tag, "carts") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope            = column[LTree]("scope")
  def referenceNumber  = column[String]("reference_number")
  def accountId        = column[Int]("account_id")
  def currency         = column[Currency]("currency")
  def subTotal         = column[Long]("sub_total")
  def shippingTotal    = column[Long]("shipping_total")
  def adjustmentsTotal = column[Long]("adjustments_total")
  def taxesTotal       = column[Long]("taxes_total")
  def grandTotal       = column[Long]("grand_total")

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
     grandTotal) <> ((Cart.apply _).tupled, Cart.unapply)
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
