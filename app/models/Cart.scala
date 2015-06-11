package models

import slick.ast.BaseTypedType
import slick.driver.PostgresDriver
import slick.lifted
import slick.lifted.AbstractTable
import utils.{TableWithIdQuery, TableWithId, Validation, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

case class Cart(id: Int, accountId: Option[Int] = None) {
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

class Carts(tag: Tag) extends TableWithId[Cart, Int](tag, "carts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Option[Int]]("account_id")
  def * = (id, accountId) <> ((Cart.apply _).tupled, Cart.unapply)
}

object Carts extends TableWithIdQuery[Carts, Cart, Int](new Carts(_)) {
  override def copyEntityId(m: Cart, id: Int): Cart = m.copy(id = id)
  val carts = this

  val tokenCardsTable = TableQuery[TokenizedCreditCards]
  val appliedPaymentsTable = TableQuery[AppliedPayments]

  // What do we return here?  I still don't have a clear STI approach in mind.  So maybe just tokenized cards for now.
  // Ideally, we would return a generic list of payment methods of all types (eg. giftcards, creditcards, store-credit)
  def findPaymentMethods(cart: Cart): Future[Seq[AppliedPayment]] = {
    val appliedpayment = AppliedPayment(id = 1, cartId = cart.id, paymentMethodId = 1, paymentMethodType = "TokenizedCard", appliedAmount = 10000, status = Applied.toString, responseCode = "")
    val appliedpayment2 = appliedpayment.copy(appliedAmount = 2550, paymentMethodId = 2)

    // The whole of the above is to have one passing token and one failing token.  So paymentMethod with ID 1 should be real.
    // PaymentMethod with ID 2 should be fake.

    Future.successful(Seq(appliedpayment, appliedpayment2))

    //val appliedIds = appliedPaymentsTable.returning(appliedPaymentsTable.map(_.paymentMethodId))

    // I tried a monadic join here and failed.
    //    val filteredPayments = for {
    //      ap <- appliedPaymentsTable if ap.cartId === cartId
    //      tc <- tokenCardsTable if tc.id === ap.paymentMethodId
    //    } yield (tc.id, tc.accountId, tc.paymentGateway, tc.gatewayTokenId, tc.lastFourDigits, tc.expirationMonth, tc.expirationYear, tc.brand)
    //    db.run(filteredPayments.head)

    // TODO: Yax or Ferdinand: Help me filter all the TokenizedCards through the mapping table of applied_payments that belong to this cart.
  }

  def addPaymentMethod(cartId: Int, paymentMethod: PaymentMethod)(implicit db: Database): Boolean = {
    true
  }

  def findById(id: Int)(implicit db: Database): Future[Option[Cart]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { carts.filter(_.id === id) }
}
