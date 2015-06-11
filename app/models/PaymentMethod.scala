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
  def authenticate(amount: Float): String Or List[ErrorMessage]
}

object PaymentMethods {
  import scala.concurrent.ExecutionContext.Implicits.global

  // ONLY implmenting tokenized payment methods right now.
  // Next up will be full credit cards
  val tokenCardsTable = TableQuery[TokenizedCreditCards]

  // TODO: The right way to do this would be to return all the different payment methods available to the user.
  def findAllByAccount(account: Shopper)(implicit db: Database): Future[Seq[TokenizedCreditCard]] = {
    db.run(tokenCardsTable.filter(_.accountId === account.id).result)
  }

  // TODO: Figure out our standard 'return' objects for all inserts and lookups
  def addPaymentTokenToAccount(paymentToken: String, account: Shopper)(implicit db: Database) : Future[TokenizedCreditCard] = {
    // First, let's get this token from stripe.
    // TODO: Let's handle a bad response from stripe and bubble up to the user
    val gateWay = StripeGateway(paymentToken = paymentToken)
    gateWay.getTokenizedCard match {
      case Success(card) =>
        val cardToSave = card.copy(accountId = account.id)
        /** Can be used like 'tokenCardsTable', but returns the newly inserted ID */
        val newlyInsertedId = tokenCardsTable.returning(tokenCardsTable.map(_.id))

        val insertAction = (newlyInsertedId += cardToSave).map { newId: Int ⇒
          /** Transform the result of the database query after it is run from Int -> TokenizedCreditCard */
          cardToSave.copy(id = newId)
        }

        db.run(insertAction)
      case Failure(t) => Future.failed(t)
    }
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
case class CreditCard(id: Int, cartId: Int, cardholderName: String, cardNumber: String, cvv: Int, status: CreditCardPaymentStatus, expiration: String, address: Address) extends PaymentMethod {
  def authenticate(amount: Float): String Or List[ErrorMessage] = {
    Good("authenticated")
  }
}
// We should probably store the payment gateway on the card itself.  This way, we can manage a world where a merchant changes processors.
case class TokenizedCreditCard(id: Int = 0, accountId: Int = 0, paymentGateway: String, gatewayTokenId: String, lastFourDigits: String, expirationMonth: Int, expirationYear: Int, brand: String) extends PaymentMethod {
  def authenticate(amount: Float): String Or List[ErrorMessage] = {
    val gateway = this.paymentGateway.toLowerCase
    if (gateway == "stripe" ) {
      StripeGateway(paymentToken = this.gatewayTokenId).authorizeAmount(this, amount.toInt)
    } else {
      Bad(List(s"Could Not Recognize Payment Gateway $gateway"))
    }
  }
}

object TokenizedCreditCard {
  def apply(card: StripeCard, tokenId: String) = {
    apply(paymentGateway = "stripe", gatewayTokenId = tokenId, lastFourDigits = card.getLast4,
          expirationMonth = card.getExpMonth, expirationYear = card.getExpYear, brand = card.getBrand)
  }
}

case class GiftCard(id: Int, cartId: Int, status: GiftCardPaymentStatus, code: String) extends PaymentMethod {
  def authenticate(amount: Float): String Or List[ErrorMessage] = {
    Good("authenticated")
  }
}

// TODO: Decide if we should take some kind of STI approach here!
class TokenizedCreditCards(tag: Tag) extends Table[TokenizedCreditCard](tag, "tokenized_credit_cards") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def accountId = column[Int]("account_id")
  def paymentGateway = column[String]("payment_gateway")
  def gatewayTokenId = column[String]("gateway_token_id")
  def lastFourDigits = column[String]("last_four_digits")
  def expirationMonth = column[Int]("expiration_month")
  def expirationYear = column[Int]("expiration_year")
  def brand = column[String]("brand")
  def * = (id, accountId, paymentGateway, gatewayTokenId, lastFourDigits, expirationMonth, expirationYear, brand) <> ((TokenizedCreditCard.apply _).tupled, TokenizedCreditCard.unapply)
}
