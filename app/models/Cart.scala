package models

import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }

import scala.concurrent.{ExecutionContext, Future}

case class Cart(id: Int = 0, accountId: Option[Int] = None, status: Cart.Status = Cart.Active) extends ModelWithIdParameter {
  def lineItemParentId = this.id

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

object Cart {
  sealed trait Status
  case object Active extends Status  // most will be here
  case object Ordered extends Status // after order
  case object Removed extends Status // admin could do this

  implicit val StatusColumnType = MappedColumnType.base[Status, String](
  {
    case t => t.toString.toLowerCase
  }, {
    case "active" => Active
    case "ordered" => Ordered
    case "removed" => Removed
    case unknown => throw new IllegalArgumentException(s"cannot map status column to type $unknown")
  })
}

class Carts(tag: Tag) extends GenericTable.TableWithId[Cart](tag, "carts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Option[Int]]("customer_id")
  def status = column[Cart.Status]("status")
  def * = (id, customerId, status) <> ((Cart.apply _).tupled, Cart.unapply)
}

object Carts extends TableQueryWithId[Cart, Carts](
  idLens = GenLens[Cart](_.id)
)(new Carts(_)) {
  val table = this

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

  def findByCustomer(customer: Customer)(implicit ec: ExecutionContext, db: Database): Future[Option[Cart]] = {
    db.run(_findByCustomer(customer).result.headOption)
  }

  def _findByCustomer(cust: Customer) = { table.filter(_.customerId === cust.id).filter(_.status === (Cart.Active: Cart.Status)) }

  // If the user doesn't have a cart yet, let's create one.
  def findOrCreateByCustomer(customer: Customer)
                            (implicit ec: ExecutionContext, db: Database): Future[Option[Cart]] = {
    val actions = for {
      numCarts <- table.filter(_.customerId === customer.id).length.result
      cart <- if (numCarts < 1) {
        val freshCart = Cart(accountId = Some(customer.id))
        (returningId += freshCart).map { id => freshCart.copy(id = id) }.map(Some(_))
      } else {
        table.filter(_.customerId === customer.id).result.headOption
      }
    } yield cart

    db.run(actions.transactionally)
  }
}
