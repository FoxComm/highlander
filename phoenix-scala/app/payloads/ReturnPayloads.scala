package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import models.returns.Return
import models.returns.ReturnLineItem.InventoryDisposition
import utils.Validation
import utils.Validation._

object ReturnPayloads {

  /* General */

  case class ReturnCreatePayload(cordRefNum: String, returnType: Return.ReturnType)

  case class ReturnUpdateStatePayload(state: Return.State, reasonId: Option[Int] = None)
      extends Validation[ReturnUpdateStatePayload] {

    def validate: ValidatedNel[Failure, ReturnUpdateStatePayload] = {
      Return.validateStateReason(state, reasonId).map { case _ ⇒ this }
    }
  }

  /* Line item updater payloads */

  case class ReturnSkuLineItemsPayload(sku: String,
                                       quantity: Int,
                                       reasonId: Int,
                                       isReturnItem: Boolean,
                                       inventoryDisposition: InventoryDisposition)
      extends Validation[ReturnSkuLineItemsPayload] {

    def validate: ValidatedNel[Failure, ReturnSkuLineItemsPayload] = {
      greaterThan(quantity, 0, "Quantity").map { case _ ⇒ this }
    }
  }

  case class ReturnGiftCardLineItemsPayload(code: String, reasonId: Int)

  case class ReturnShippingCostLineItemsPayload(reasonId: Int)

  /* Payment payloads */

  case class ReturnPaymentPayload(amount: Int) extends Validation[ReturnPaymentPayload] {

    def validate: ValidatedNel[Failure, ReturnPaymentPayload] = {
      greaterThan(amount, 0, "Amount").map { case _ ⇒ this }
    }
  }

  /* Misc */

  case class ReturnMessageToCustomerPayload(message: String)
      extends Validation[ReturnMessageToCustomerPayload] {

    def validate: ValidatedNel[Failure, ReturnMessageToCustomerPayload] = {
      (greaterThanOrEqual(message.length, 0, "Message length") |@| lesserThanOrEqual(
              message.length,
              Return.messageToAccountMaxLength,
              "Message length")).map {
        case _ ⇒ this
      }
    }
  }
}
