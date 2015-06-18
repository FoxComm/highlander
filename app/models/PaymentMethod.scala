package models

import com.stripe.model.{Card ⇒ StripeCard}
import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}
import org.scalactic._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

import scala.concurrent.{ExecutionContext, Future}

abstract class PaymentMethod {
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]]
}

object PaymentMethods {

  // TODO: Make polymorphic for real.
  def findById(id: Int)(implicit db: Database): Future[Option[CreditCardGateway]] = {
    CreditCardGateways.findById(id)
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
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    Future.successful(Good("authenticated"))
  }
}
case class GiftCard(id: Int, orderId: Int, status: GiftCardPaymentStatus, code: String) extends PaymentMethod {
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or List[ErrorMessage]] = {
    Future.successful(Good("authenticated"))
  }
}

