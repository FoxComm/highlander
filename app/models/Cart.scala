package models

import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }

import scala.concurrent.Future

case class Cart(id: Int, accountId: Option[Int] = None) extends LineItemable with ModelWithIdParameter {
  override type Id = Int

  def lineItemParentId = this.id

  val lineItems: Seq[LineItem] = Seq.empty
  //val payments: Seq[AppliedPayment] = Seq.empty
  // val fulfillments: Seq[Fulfillment] = Seq.empty

  //  def coupons: Seq[Coupon] = Seq.empty
  //  def adjustments: Seq[Adjustment] = Seq.empty

  // TODO: how do we handle adjustment/coupon
  // specifically, promotions are handled at the checkout level, but need to display in the cart
  //def addCoupon(coupon: Coupon) = {}

  // carts support guest checkout
  def isGuest = this.accountId.isDefined

  // TODO: service class it?

  def payments: Future[Seq[AppliedPayment]] = {
    Carts.findPaymentMethods(this)
  }

  def subTotal: Int = {
    10000 //in cents?
  }

  def grandTotal: Int = {
    12550
  }

  def toMap: Map[String, Any] = {
    val fields = this.getClass.getDeclaredFields.map(_.getName)
    val values = Cart.unapply(this).get.productIterator.toSeq
    fields.zip(values).toMap
  }
}

class Carts(tag: Tag) extends GenericTable.TableWithId[Cart](tag, "carts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Option[Int]]("customer_id")
  def * = (id, customerId) <> ((Cart.apply _).tupled, Cart.unapply)
}

object Carts extends TableQueryWithId[Cart, Carts](
  idLens = GenLens[Cart](_.id)
)(new Carts(_)) {
  val carts = this

  val tokenCardsTable = TableQuery[TokenizedCreditCards]
  val appliedPaymentsTable = TableQuery[AppliedPayments]

  // What do we return here?  I still don't have a clear STI approach in mind.  So maybe just tokenized cards for now.
  // Ideally, we would return a generic list of payment methods of all types (eg. giftcards, creditcards, store-credit)
  def findPaymentMethods(cart: Cart): Future[Seq[AppliedPayment]] = {
    val appliedpayment = AppliedPayment(id = 1, cartId = cart.id, paymentMethodId = 1, paymentMethodType = "TokenizedCard", appliedAmount = 10000, status = Applied.toString, responseCode = "")
    val appliedpayment2 = appliedpayment.copy(appliedAmount = 2550, paymentMethodId = 2)


    Future.successful(Seq(appliedpayment, appliedpayment2))

  }

  def addPaymentMethod(cartId: Int, paymentMethod: PaymentMethod)(implicit db: Database): Boolean = {
    true
  }

  def findById(id: Int)(implicit db: Database): Future[Option[Cart]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { carts.filter(_.id === id) }

  def findByCustomer(customer: Customer)(implicit db: Database): Future[Option[Cart]] = {
    db.run(_findByCustomer(customer).result.headOption)
  }

  def _findByCustomer(cust: Customer) = {carts.filter(_.customerId === cust.id)}
}
