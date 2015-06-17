package models

import utils.{Validation, RichTable}
import payloads.CreateAddressPayload
import services.StripeGateway

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Failure, Success}
import com.stripe.model.{Card => StripeCard}

abstract class PaymentMethod {
  def authenticate(amount: Float)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]]
}

object PaymentMethods {
  import scala.concurrent.ExecutionContext.Implicits.global

  // ONLY implmenting tokenized payment methods right now.
  // Next up will be full credit cards
  val tokenCardsTable = TableQuery[TokenizedCreditCards]

  // TODO: The right way to do this would be to return all the different payment methods available to the user.
  def findAllByCustomer(customer: Customer)(implicit db: Database): Future[Seq[TokenizedCreditCard]] = {
    db.run(tokenCardsTable.filter(_.customerId === customer.id).result)
  }

  // TODO: Make polymorphic for real.
  def findById(id: Int)(implicit db: Database): Future[Option[TokenizedCreditCard]] = {
    db.run(tokenCardsTable.filter(_.id === id).result.headOption)
  }
}

sealed trait PaymentStatus

sealed trait CreditCardPaymentStatus extends PaymentStatus
case object Applied extends CreditCardPaymentStatus
case object Auth extends CreditCardPaymentStatus
case object FailedCapture extends CreditCardPaymentStatus
case object CanceledAuth extends CreditCardPaymentStatus
case object ExpiredAuth extends CreditCardPaymentStatus

sealed trait GiftCardPaymentStatus extends PaymentStatus
case object InsufficientBalance extends GiftCardPaymentStatus
case object SuccessfulDebit extends GiftCardPaymentStatus
case object FailedDebit extends GiftCardPaymentStatus

// TODO: Figure out how to have the 'status' field on the payment and not the payment method.
case class CreditCard(id: Int, orderId: Int, cardholderName: String, cardNumber: String, cvv: Int, status: CreditCardPaymentStatus, expiration: String, address: Address) extends PaymentMethod {
  def authenticate(amount: Float)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    Future.successful(Good("authenticated"))
  }
}
// We should probably store the payment gateway on the card itself.  This way, we can manage a world where a merchant changes processors.
case class TokenizedCreditCard(id: Int = 0, customerId: Int = 0, paymentGateway: String, gatewayTokenId: String, lastFourDigits: String, expirationMonth: Int, expirationYear: Int, brand: String) extends PaymentMethod {
  def authenticate(amount: Float)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    val gateway = this.paymentGateway.toLowerCase
    if (gateway == "stripe" ) {
      StripeGateway().authorizeAmount(this, amount.toInt)
    } else {
      Future.successful(Bad(List(s"Could Not Recognize Payment Gateway $gateway")))
    }
  }
}

object TokenizedCreditCard {
  def fromStripe(card: StripeCard, tokenId: String): TokenizedCreditCard = {
    apply(paymentGateway = "stripe", gatewayTokenId = tokenId, lastFourDigits = card.getLast4,
          expirationMonth = card.getExpMonth, expirationYear = card.getExpYear, brand = card.getBrand)
  }
}

case class GiftCard(id: Int, orderId: Int, status: GiftCardPaymentStatus, code: String) extends PaymentMethod {
  def authenticate(amount: Float)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    Future.successful(Good("authenticated"))
  }
}

// TODO: Decide if we should take some kind of STI approach here!
class TokenizedCreditCards(tag: Tag) extends Table[TokenizedCreditCard](tag, "tokenized_credit_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def paymentGateway = column[String]("payment_gateway")
  def gatewayTokenId = column[String]("gateway_token_id")
  def lastFourDigits = column[String]("last_four_digits")
  def expirationMonth = column[Int]("expiration_month")
  def expirationYear = column[Int]("expiration_year")
  def brand = column[String]("brand")
  def * = (id, customerId, paymentGateway, gatewayTokenId, lastFourDigits, expirationMonth, expirationYear, brand) <> ((TokenizedCreditCard.apply _).tupled, TokenizedCreditCard.unapply)
}
