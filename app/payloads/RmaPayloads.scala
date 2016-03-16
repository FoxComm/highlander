package payloads

import cats.data._
import cats.implicits._
import models.rma.{RmaLineItem, Rma}
import RmaLineItem.InventoryDisposition
import models.rma.Rma
import services.Failure
import utils.Litterbox._
import utils.Money.Currency
import utils.Validation
import Validation._

/* General */

final case class RmaCreatePayload(orderRefNum: String, rmaType: Rma.RmaType)

final case class RmaUpdateStatePayload(state: Rma.State, reasonId: Option[Int] = None)
  extends Validation[RmaUpdateStatePayload] {

  def validate: ValidatedNel[Failure, RmaUpdateStatePayload] = {
    Rma.validateStateReason(state, reasonId).map { case _ ⇒ this }
  }
}

/* Line item updater payloads */

final case class RmaSkuLineItemsPayload(sku: String, quantity: Int, reasonId: Int, isReturnItem: Boolean,
  inventoryDisposition: InventoryDisposition) extends Validation[RmaSkuLineItemsPayload] {

  def validate: ValidatedNel[Failure, RmaSkuLineItemsPayload] = {
    greaterThan(quantity, 0, "Quantity").map { case _ ⇒ this }
  }
}

final case class RmaGiftCardLineItemsPayload(code: String, reasonId: Int)

final case class RmaShippingCostLineItemsPayload(reasonId: Int)

/* Payment payloads */

final case class RmaPaymentPayload(amount: Int) extends Validation[RmaPaymentPayload] {

  def validate: ValidatedNel[Failure, RmaPaymentPayload] = {
    greaterThan(amount, 0, "Amount").map { case _ ⇒ this }
  }
}

final case class RmaCcPaymentPayload(amount: Int) extends Validation[RmaCcPaymentPayload] {

  def validate: ValidatedNel[Failure, RmaCcPaymentPayload] = {
    greaterThan(amount, 0, "Amount").map { case _ ⇒ this }
  }
}

/* Misc */

final case class RmaMessageToCustomerPayload(message: String) extends Validation[RmaMessageToCustomerPayload] {

  def validate: ValidatedNel[Failure, RmaMessageToCustomerPayload] = {
    (greaterThanOrEqual(message.length , 0, "Message length")
      |@| lesserThanOrEqual(message.length, Rma.messageToCustomerMaxLength, "Message length")
      ).map { case _ ⇒ this }
  }
}
