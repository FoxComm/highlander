package models

import com.pellucid.sealerate
import com.stripe.model.{Card ⇒ StripeCard}
import com.wix.accord.dsl.{validator ⇒ createValidator}
import com.wix.accord.{Failure ⇒ ValidationFailure}
import org.scalactic._
import services.{Failures, Failure}
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}

import scala.concurrent.{ExecutionContext, Future}

import utils.ADT

abstract class PaymentMethod {
  def authorize(amount: Int)(implicit ec: ExecutionContext): Future[String Or Failures]
}

object PaymentMethods {

  // TODO: Make polymorphic for real.
  def findById(id: Int)(implicit db: Database): Future[Option[CreditCard]] = {
    CreditCards.findById(id)
  }
}

sealed trait PaymentStatus

sealed trait CreditCardPaymentStatus extends PaymentStatus
case object Applied extends CreditCardPaymentStatus
case object Auth extends CreditCardPaymentStatus
case object FailedCapture extends CreditCardPaymentStatus
case object CanceledAuth extends CreditCardPaymentStatus
case object ExpiredAuth extends CreditCardPaymentStatus

object CreditCardPaymentStatus extends ADT[CreditCardPaymentStatus] {
  def types = sealerate.values[CreditCardPaymentStatus]
}


