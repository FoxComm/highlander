package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import models.payment.PaymentMethod
import models.returns.{Return, ReturnLineItem, ReturnReason}
import models.returns.ReturnLineItem.InventoryDisposition
import utils.{ADTTypeHints, Validation}
import utils.Validation._

object ReturnPayloads {

  /* General */

  case class ReturnCreatePayload(cordRefNum: String, returnType: Return.ReturnType)

  case class ReturnUpdateStatePayload(state: Return.State, reasonId: Option[Int] = None)

  /* Line item updater payloads */

  sealed trait ReturnLineItemPayload extends Validation[ReturnLineItemPayload] {
    def reasonId: Int

    def validate: ValidatedNel[Failure, ReturnLineItemPayload] = this.valid
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
      extends ReturnLineItemPayload {

    override def validate: ValidatedNel[Failure, ReturnLineItemPayload] =
      greaterThan(quantity, 0, "Quantity").map(_ ⇒ this)
  }

  case class ReturnGiftCardLineItemPayload(code: String, reasonId: Int)
      extends ReturnLineItemPayload

  case class ReturnShippingCostLineItemPayload(amount: Int, reasonId: Int)
      extends ReturnLineItemPayload {
    override def validate: ValidatedNel[Failure, ReturnLineItemPayload] =
      greaterThan(amount, 0, "Amount").map(_ ⇒ this)
  }

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

  case class ReturnReasonPayload(name: String) extends Validation[ReturnReasonPayload] {
    val reasonNameMaxLength = 255

    def validate: ValidatedNel[Failure, ReturnReasonPayload] = {
      val clue = "Reason name length"
      (greaterThan(name.length, 0, clue) |@| lesserThanOrEqual(name.length,
                                                               reasonNameMaxLength,
                                                               clue)).map {
        case _ ⇒ this
      }
    }
  }
}
