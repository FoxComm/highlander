package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import models.payment.PaymentMethod
import models.returns.{Return, ReturnLineItem}
import models.returns.ReturnLineItem.InventoryDisposition
import org.json4s.CustomSerializer
import utils.{ADTTypeHints, Validation}
import utils.Validation._

object ReturnPayloads {

  /* General */

  case class ReturnCreatePayload(cordRefNum: String, returnType: Return.ReturnType)

  case class ReturnUpdateStatePayload(state: Return.State, reasonId: Option[Int] = None)
      extends Validation[ReturnUpdateStatePayload] {

    def validate: ValidatedNel[Failure, ReturnUpdateStatePayload] = {
      Return.validateStateReason(state, reasonId).map(_ ⇒ this)
    }
  }

  /* Line item updater payloads */

  sealed trait ReturnLineItemPayload {
    def reasonId: Int
  }
  object ReturnLineItemPayload {
    def typeHints =
      ADTTypeHints(
          Map(
              ReturnLineItem.GiftCardItem → classOf[ReturnGiftCardLineItemPayload],
              ReturnLineItem.ShippingCost → classOf[ReturnShippingCostLineItemPayload],
              ReturnLineItem.SkuItem      → classOf[ReturnSkuLineItemPayload]
          ))
  }

  case class ReturnSkuLineItemPayload(sku: String,
                                      quantity: Int,
                                      reasonId: Int,
                                      isReturnItem: Boolean,
                                      inventoryDisposition: InventoryDisposition)
      extends ReturnLineItemPayload
      with Validation[ReturnSkuLineItemPayload] {

    def validate: ValidatedNel[Failure, ReturnSkuLineItemPayload] = {
      greaterThan(quantity, 0, "Quantity").map { case _ ⇒ this }
    }
  }

  case class ReturnGiftCardLineItemPayload(code: String, reasonId: Int)
      extends ReturnLineItemPayload

  case class ReturnShippingCostLineItemPayload(reasonId: Int) extends ReturnLineItemPayload

  /* Payment payloads */

  case class ReturnPaymentPayload(amount: Int, method: PaymentMethod.Type)
      extends Validation[ReturnPaymentPayload] {

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
