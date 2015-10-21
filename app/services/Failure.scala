package services

import scala.collection.immutable

import com.stripe.exception.StripeException
import models.{CreditCard, GiftCard, Order, StoreCredit}
import services.Util.searchTerm
import utils.friendlyClassName

sealed trait Failure {
  def description: immutable.Traversable[String]
}

final case class NotFoundFailure404(message: String) extends Failure {
  override def description = List(message)
}

object NotFoundFailure404 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure404 = {
    NotFoundFailure404(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }
}

final case class NotFoundFailure400(message: String) extends Failure {
  override def description = List(message)
}

object NotFoundFailure400 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure400 = {
    NotFoundFailure400(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }
}

final case class StripeFailure(exception: StripeException) extends Failure {
  override def description = List(exception.getMessage)
}

case object CVCFailure extends Failure {
  override def description = List("failed CVC check")
}

final case class GeneralFailure(a: String) extends Failure {
  override def description = List(a)
}

case object EmptyCancellationReasonFailure extends Failure {
  override def description = List("Please provide valid cancellation reason")
}

case object InvalidCancellationReasonFailure extends Failure {
  override def description = List("Cancellation reason doesn't exist")
}

case object OpenTransactionsFailure extends Failure {
  override def description = List("Open transactions should be canceled/completed")
}

case object CustomerHasDefaultShippingAddress extends Failure {
  override def description = List("customer already has default shipping address")
}

case object CustomerHasDefaultCreditCard extends Failure {
  override def description = List("customer already has default credit card")
}

final case class CustomerHasCart(id: Int) extends Failure {
  override def description = List(s"customer with id=$id already has an active cart")
}

final case class TransitionNotAllowed(from: String, to: String) extends Failure {
  override def description = List(s"Transition from $from to $to is not allowed")
}

final case class OrderUpdateFailure(referenceNumber: String, reason: String) extends Failure {
  override def description = List(reason)
}

final case class GiftCardConvertFailure(gc: GiftCard) extends Failure {
  override def description = List(s"cannot convert a gift card with status '${gc.status}'")
}

final case class StoreCreditConvertFailure(sc: StoreCredit) extends Failure {
  override def description = List(s"cannot convert a store credit with status '${sc.status}'")
}

object OrderPaymentNotFoundFailure {
  def apply[M](m: M): NotFoundFailure400 = NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
}

final case class GiftCardNotEnoughBalance(gc: GiftCard, requestedAmount: Int) extends Failure {
  override def description =
    List(s"giftCard has availableBalance=${gc.availableBalance} less than requestedAmount=$requestedAmount")
}

final case class GiftCardIsInactive(gc: GiftCard) extends Failure {
  override def description = List(s"giftCard with id=${gc.id} is inactive")
}

final case class CannotUseInactiveCreditCard(cc: CreditCard) extends Failure {
  override def description = List(s"creditCard with id=${cc.id} is inactive")
}

final case class OrderLockedFailure(referenceNumber: String) extends Failure {
  override def description = List("Order is locked")
}

final case class CustomerHasInsufficientStoreCredit(id: Int, has: Int, want: Int) extends Failure {
  override def description = List(s"customer with id=$id has storeCredit=$has less than requestedAmount=$want")
}

final case class OrderShippingMethodsCannotBeProcessed(referenceNumber: String) extends Failure {
  override def description = List(s"Shipping methods for order ${referenceNumber} cannot be processed")
}

final case class ShippingMethodDoesNotExist(shippingMethodId: Int) extends Failure {
  override def description = List(s"Shipping method ${shippingMethodId} can't be added because it doesn't exist")
}

final case class ShippingMethodNotApplicableToOrder(shippingMethodId: Int, referenceNumber: String) extends Failure {
  override def description = List(s"Shipping method ${shippingMethodId} is not applicable to order ${referenceNumber}")
}

case object CreditCardMustHaveAddress extends Failure {
  override def description = List("cannot create creditCard without an address")
}

final case class StripeRuntimeException[E <: StripeException](exception: E) extends Failure {
  val description = List(exception.getMessage)
}

object Util {
  def searchTerm[A](a: A): String = a match {
    case Order ⇒ "refNum"
    case GiftCard ⇒ "code"
    case _ ⇒ "id"
  }
}