package services

import collection.immutable
import com.stripe.exception.StripeException
import models.{CreditCard, GiftCard, Order}
import utils.{ModelWithIdParameter, Validation}
import utils.friendlyClassName

sealed trait Failure {
  def description: immutable.Traversable[String]
}

final case class NotFoundFailure(message: String) extends Failure {
  override def description = List(message)
}

object NotFoundFailure {
  def apply[M <: ModelWithIdParameter](m: M): NotFoundFailure =
    NotFoundFailure(s"${m.modelName} with id=${m.id} not found")

  def apply[A](a: A, id: Int): NotFoundFailure =
    NotFoundFailure(s"${friendlyClassName(a)} with id=$id not found")
}

final case class StripeFailure(exception: StripeException) extends Failure {
  override def description = List(exception.getMessage)
}

final case class ValidationFailure(violation: Validation.Result.Failure) extends Failure {
  override def description = violation.messages.map(_.toString)
}

final case class GeneralFailure(a: String) extends Failure {
  override def description = List(a)
}

final case object CustomerHasDefaultShippingAddress extends Failure {
  override def description = List("customer already has default shipping address")
}

final case object CustomerHasDefaultCreditCard extends Failure {
  override def description = List("customer already has default credit card")
}

final case class OrderUpdateFailure(referenceNumber: String, reason: String) extends Failure {
  override def description = List(reason)
}

object OrderNotFoundFailure {
  def apply(order: Order): NotFoundFailure = apply(order.referenceNumber)
  def apply(refNum: String): NotFoundFailure = NotFoundFailure(s"order with referenceNumber=$refNum not found")
}

object GiftCardNotFoundFailure {
  def apply(giftCard: GiftCard): NotFoundFailure = apply(giftCard.code)
  def apply(code: String): NotFoundFailure = NotFoundFailure(s"giftCard with code=$code not found")
}

final case class GiftCardNotEnoughBalance(gc: GiftCard, requestedAmount: Int) extends Failure {
  override def description =
    List(s"giftCard has availableBalance=${gc.availableBalance} less than requestedAmount=$requestedAmount")
}

final case class CannotUseInactiveCreditCard(cc: CreditCard) extends Failure {
  override def description = List(s"creditCard with id=${cc.id} is inactive")
}

final case class CartAlreadyHasCreditCard(order: Order) extends Failure {
  override def description = List(s"order with referenceNumber=${order.referenceNumber} already has a credit card")
}

final case class CustomerHasInsufficientStoreCredit(id: Int, has: Int, want: Int) extends Failure {
  override def description = List(s"customer with id=$id has storeCredit=$has less than requestedAmount=$want")
}
