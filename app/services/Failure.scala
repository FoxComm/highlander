package services

import com.stripe.exception.StripeException
import models.activity.Dimension
import models.order.Order
import models.inventory.Sku
import models.payment.creditcard.CreditCard
import models.payment.giftcard.GiftCard
import models.payment.storecredit.StoreCredit
import models.rma.Rma
import responses.BatchMetadata
import services.Util.searchTerm
import utils.friendlyClassName

sealed trait Failure {
  def description: String
}

final case class NotFoundFailure404(message: String) extends Failure {
  override def description = message
}

object NotFoundFailure404 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure404 = {
    NotFoundFailure404(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }
}

final case class NotFoundFailure400(message: String) extends Failure {
  override def description = message
}

object NotFoundFailure400 {
  def apply[A](a: A, searchKey: Any): NotFoundFailure400 = {
    NotFoundFailure400(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey not found")
  }
}

final case class DatabaseFailure(message: String) extends Failure {
  override def description = message
}

final case class InvalidReasonTypeFailure(name: String) extends Failure {
  override def description = s"Reason type named '${name}' doesn't exist"
}

final case class InvalidFieldFailure(name: String) extends Failure {
  override def description = s"Invalid value for field '${name}' provided"
}

final case class GeneralFailure(a: String) extends Failure {
  override def description = a
}

final case class ShipmentNotFoundFailure(orderRefNum: String) extends Failure {
  override def description = s"No shipments found for order with refNum=$orderRefNum"
}

case object EmptyCancellationReasonFailure extends Failure {
  override def description = "Please provide valid cancellation reason"
}

case object EmptyRefNumFailure extends Failure {
  override def description = "Please provide an order reference number"
}

case object InvalidCancellationReasonFailure extends Failure {
  override def description = "Cancellation reason doesn't exist"
}

case object OpenTransactionsFailure extends Failure {
  override def description = "Open transactions should be canceled/completed"
}

case object CustomerHasDefaultShippingAddress extends Failure {
  override def description = "customer already has default shipping address"
}

case object CustomerHasDefaultCreditCard extends Failure {
  override def description = "customer already has default credit card"
}

final case class CustomerHasNoDefaultAddress(customerId: Int) extends Failure {
  override def description = s"No default address found for customer with id =$customerId"
}

final case class StateTransitionNotAllowed(message: String) extends Failure {
  override def description = message
}

object StateTransitionNotAllowed {
  def apply[A](a: A, fromState: String, toState: String, searchKey: Any): StateTransitionNotAllowed = {
    StateTransitionNotAllowed(s"Transition from $fromState to $toState is not allowed for ${friendlyClassName(a)} " +
      s"with ${searchTerm(a)}=$searchKey")
  }

  def apply(from: Order.State, to: Order.State, refNum: String): StateTransitionNotAllowed = {
    apply(Order, from.toString, to.toString, refNum)
  }
}

final case class NotificationTrailNotFound400(adminId: Int) extends Failure {
  override def description = s"Notification trail for adminId=$adminId not found"
}

case object CustomerEmailNotUnique extends Failure {
  override def description = "The email address you entered is already in use"
}

final case class OrderUpdateFailure(referenceNumber: String, reason: String) extends Failure {
  override def description = reason
}

object RmaFailures {
  final case class EmptyRma(refNum: String) extends Failure {
    override def description = s"rma with referenceNumber=$refNum has no line items"
  }

  final case class SkuNotFoundInOrder(sku: String, refNum: String) extends Failure {
    override def description = s"line item with sku=$sku not found in order with referenceNumber=$refNum"
  }

  final case class SkuNotFoundInContext(sku: String, context: String) extends Failure {
    override def description = s"line item with sku=$sku not found in context with $context"
  }
}

object CartFailures {
  final case class OrderMustBeCart(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is not in cart state"
  }

  final case class CustomerHasCart(id: Int) extends Failure {
    override def description = s"customer with id=$id already has an active cart"
  }

  final case class CustomerHasNoActiveOrder(customerId: Int) extends Failure {
    override def description = s"customer with id=$customerId has no active order"
  }

  final case class EmptyCart(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum is an empty cart"
  }

  final case class NoShipAddress(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has no shipping address"
  }

  final case class NoShipMethod(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has no shipping method"
  }

  final case class InvalidShippingMethod(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has invalid shipping method"
  }

  final case class InsufficientFunds(refNum: String) extends Failure {
    override def description = s"order with referenceNumber=$refNum has insufficient funds"
  }
}

final case class GiftCardMustBeCart(code: String) extends Failure {
  override def description = s"giftCart with code=$code is not in cart state"
}

final case class GiftCardMustNotBeCart(code: String) extends Failure {
  override def description = s"giftCart with code=$code must not be in cart state"
}

final case class GiftCardConvertFailure(gc: GiftCard) extends Failure {
  override def description = s"cannot convert a gift card with state '${gc.state}'"
}

final case class StoreCreditConvertFailure(sc: StoreCredit) extends Failure {
  override def description = s"cannot convert a store credit with state '${sc.state}'"
}

final case class OrderAssigneeNotFound(refNum: String, assigneeId: Int) extends Failure {
  override def description = s"storeAdmin with id=$assigneeId is not assigned to order with refNum=$refNum"
}

final case class CustomerAssigneeNotFound(customerId: Int, assigneeId: Int) extends Failure {
  override def description = s"storeAdmin with id=$assigneeId is not assigned to customer with id=$customerId"
}

final case class GiftCardAssigneeNotFound(code: String, assigneeId: Int) extends Failure {
  override def description = s"storeAdmin with id=$assigneeId is not assigned to giftCard with code=$code"
}

final case class SharedSearchAssociationNotFound(code: String, associateId: Int) extends Failure {
  override def description = s"sharedSearch with code=$code is not associated to storeAdmin with id=$associateId"
}

final case class OrderWatcherNotFound(refNum: String, assigneeId: Int) extends Failure {
  override def description = s"storeAdmin with id=$assigneeId is not watching order with refNum=$refNum"
}

final case class CustomerWatcherNotFound(customerId: Int, assigneeId: Int) extends Failure {
  override def description = s"storeAdmin with id=$assigneeId is not watching to customer with id=$customerId"
}

final case class GiftCardWatcherNotFound(code: String, assigneeId: Int) extends Failure {
  override def description = s"storeAdmin with id=$assigneeId is not watching to giftCard with code=$code"
}


final case class RmaAssigneeNotFound(refNum: String, assigneeId: Int) extends Failure {
  override def description = s"storeAdmin with id=$assigneeId is not assigned to RMA with refNum=$refNum"
}

case object LoginFailed extends Failure {
  override def description = s"Email or password invalid"
}

case object SharedSearchInvalidQueryFailure extends Failure {
  override def description = s"Invalid JSON provided for shared search query"
}

final case class GiftCardPaymentAlreadyAdded(refNum: String, code: String) extends Failure {
  override def description = s"giftCard with code=$code already added as payment method to order with refNum=$refNum"
}

object OrderPaymentNotFoundFailure {
  def apply[M](m: M): NotFoundFailure400 = NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
}

object RmaPaymentNotFoundFailure {
  def apply[M](m: M): NotFoundFailure400 = NotFoundFailure400(s"${friendlyClassName(m)} payment not found")
}

final case class GiftCardNotEnoughBalance(gc: GiftCard, requestedAmount: Int) extends Failure {
  override def description =
    s"giftCard with code=${gc.code} has availableBalance=${gc.availableBalance} less than requestedAmount=$requestedAmount"
}

final case class GiftCardIsInactive(gc: GiftCard) extends Failure {
  override def description = s"giftCard with id=${gc.id} is inactive"
}

final case class StoreCreditIsInactive(sc: StoreCredit) extends Failure {
  override def description = s"storeCredit with id=${sc.id} is inactive"
}

final case class CannotUseInactiveCreditCard(cc: CreditCard) extends Failure {
  override def description = s"creditCard with id=${cc.id} is inactive"
}

final case class LockedFailure(message: String) extends Failure {
  override def description = message
}

object LockedFailure {
  def apply[A](a: A, searchKey: Any): LockedFailure = {
    LockedFailure(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey is locked")
  }
}

final case class NotLockedFailure(message: String) extends Failure {
  override def description = message
}

object NotLockedFailure {
  def apply[A](a: A, searchKey: Any): NotLockedFailure = {
    NotLockedFailure(s"${friendlyClassName(a)} with ${searchTerm(a)}=$searchKey is not locked")
  }
}

final case class CustomerHasInsufficientStoreCredit(id: Int, has: Int, want: Int) extends Failure {
  override def description = s"customer with id=$id has storeCredit=$has less than requestedAmount=$want"
}

final case class ShippingMethodIsNotFound(shippingMethodId: Int) extends Failure {
  override def description = s"Shipping method $shippingMethodId can't be found"
}

final case class ShippingMethodIsNotActive(shippingMethodId: Int) extends Failure {
  override def description = s"Shipping method $shippingMethodId can't be added because it's not active"
}

final case class ShippingMethodNotApplicableToOrder(shippingMethodId: Int, referenceNumber: String) extends Failure {
  override def description = s"Shipping method $shippingMethodId is not applicable to order $referenceNumber"
}

case object CreditCardMustHaveAddress extends Failure {
  override def description = "cannot create creditCard without an address"
}

final case class CustomerHasNoCreditCard(customerId: Int) extends Failure {
  override def description = s"No credit card found for customer with id=$customerId"
}

final case class AlreadySavedForLater(customerId: Int, skuId: Int) extends Failure {
  override def description = s"Customer with id=$customerId already has SKU with id=$skuId saved for later"
}

object ProductFailure { 

  final case class SkuFormsAndShadowsNotSameSize() extends Failure {
    override def description = s"The lists of sku forms and shadows are not the same size in the payload"
  }

  final case class SkuShadowNotFoundInPayload(code: String) extends Failure {
    override def description = s"Sku shadow with code $code cannot be found in the payload"
  }

  final case class SkuNotFound(code: String) extends Failure {
    override def description = s"Sku with code $code cannot be found"
  }

  final case class SkuNotFoundForContext(code: String, productContext: String) extends Failure {
    override def description = s"Sku $code with product context $productContext cannot be found"
  }

  final case class ProductFormNotFound(formId: Int) extends Failure {
    override def description = s"Product form with id=$formId cannot be found"
  }

  final case class ProductNotFoundForContext(productId: Int, productContextId: Int) extends Failure {
    override def description = s"Product with id=$productId with product context $productContextId cannot be found"
  }

  final case class ObjectContextNotFound(name: String) extends Failure {
    override def description = s"Product Context with name $name cannot be found"
  }

  final case class ProductShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Product shadow has an invalid attribute $key with value $value"
  }

  final case class ProductShadowAttributeNotAString(key: String) extends Failure {
    override def description = s"Product shadow attribute $key must be a string"
  }

  final case class ProductAttributesAreEmpty() extends Failure {
    override def description = s"Product attributes are empty"
  }
  final case class ProductShadowAttributesAreEmpty() extends Failure {
    override def description = s"Product shadow attributes are empty"
  }

  final case class NoVariantForContext(context: String) extends Failure {
    override def description = s"No variant context $context"
  }

}

object IlluminateFailure { 

  final case class ShadowHasInvalidAttribute(key: String, value: String) extends Failure {
    override def description = s"Shadow has an invalid attribute $key with value $value"
  }
  final case class ShadowAttributeMissingRef(key: String) extends Failure {
    override def description = s"Shadow attribute $key is missing 'ref'"
  }

  final case class AttributesAreEmpty() extends Failure {
    override def description = s"Form attributes are empty"
  }
  final case class ShadowAttributesAreEmpty() extends Failure {
    override def description = s"Shadow attributes are empty"
  }

}

final case class InventorySummaryNotFound(skuId: Int, warehouseId: Int) extends Failure {
  override def description = s"Summary for sku with id=${skuId} in warehouse with id=${warehouseId} not found"
}

object CreditCardFailure {
  final case class StripeFailure(exception: StripeException) extends Failure {
    override def description = exception.getMessage
  }

  case object InvalidCvc extends Failure {
    override def description = "failed CVC check"
  }

  case object IncorrectCvc extends Failure {
    override def description = "The credit card's cvc is incorrect"
  }

  case object MonthExpirationInvalid extends Failure {
    override def description = "The credit card's month expiration is invalid"
  }

  case object YearExpirationInvalid extends Failure {
    override def description = "The credit card's year expiration is invalid"
  }

  case object IncorrectNumber extends Failure {
    override def description = "The credit card's number is incorrect"
  }

  case object InvalidNumber extends Failure {
    override def description = "The card number is not a valid credit card number"
  }

  case object ExpiredCard extends Failure {
    override def description = "The credit card is expired"
  }

  case object IncorrectZip extends Failure {
    override def description = "The zip code failed verification"
  }

  case object CardDeclined extends Failure {
    override def description = "The credit card was declined"
  }

  case object Missing extends Failure {
    override def description = "Could not find a credit card for the customer"
  }

  case object ProcessingError extends Failure {
    override def description = "There was an error processing the credit card request"
  }
}

object Util {
  def searchTerm[A](a: A): String = a match {
    case Order | _: Order | Rma | _: Rma ⇒ "referenceNumber"
    case GiftCard | _: GiftCard | Sku | _: Sku ⇒ "code"
    case Dimension | _: Dimension ⇒ "name"
    case _ ⇒ "id"
  }

  def diffToBatchErrors[A, B](requested: Seq[A], available: Seq[A], modelType: B): BatchMetadata.FailureData =
    requested.diff(available).map(id ⇒ (id.toString, NotFoundFailure404(modelType, id).description)).toMap

  /* Diff lists of model identifiers to produce a list of failures for absent models */
  def diffToFailures[A, B](requested: Seq[A], available: Seq[A], modelType: B): Option[Failures] =
    Failures(requested.diff(available).map(NotFoundFailure404(modelType, _)): _*)

  /* Diff lists of model identifiers to produce a list of warnings for absent models */
  def diffToFlatFailures[A, B](requested: Seq[A], available: Seq[A], modelType: B): Option[List[String]] =
    diffToFailures(requested, available, modelType).map(_.flatten)
}
