package phoenix.payloads

import cats.data._
import cats.implicits._
import core.failures.Failure
import core.utils.Validation
import core.utils.Validation._
import phoenix.failures.{EmptyCancellationReasonFailure, NonEmptyCancellationReasonFailure}
import phoenix.models.payment.PaymentMethod
import phoenix.models.returns.{Return, ReturnLineItem}
import phoenix.utils.ADTTypeHints

object ReturnPayloads {

  /* General */

  case class ReturnCreatePayload(cordRefNum: String, returnType: Return.ReturnType)

  case class ReturnUpdateStatePayload(state: Return.State, reasonId: Option[Int])
      extends Validation[ReturnUpdateStatePayload] {
    def validate: ValidatedNel[Failure, ReturnUpdateStatePayload] =
      (Validation.ok |+|
        Validation.isInvalid(state == Return.Canceled && reasonId.isEmpty, EmptyCancellationReasonFailure) |+|
        Validation.isInvalid(state != Return.Canceled && reasonId.isDefined,
                             NonEmptyCancellationReasonFailure)).map(_ ⇒ this)
  }

  /* Line item updater payloads */

  sealed trait ReturnLineItemPayload extends Validation[ReturnLineItemPayload] {
    def reasonId: Int
  }
  object ReturnLineItemPayload {
    def typeHints =
      ADTTypeHints(
        Map(
          ReturnLineItem.ShippingCost → classOf[ReturnShippingCostLineItemPayload],
          ReturnLineItem.SkuItem      → classOf[ReturnSkuLineItemPayload]
        ))
  }

  case class ReturnSkuLineItemPayload(sku: String, quantity: Int, reasonId: Int)
      extends ReturnLineItemPayload {
    def validate: ValidatedNel[Failure, ReturnLineItemPayload] =
      greaterThan(quantity, 0, "Quantity").map(_ ⇒ this)
  }

  case class ReturnShippingCostLineItemPayload(amount: Long, reasonId: Int) extends ReturnLineItemPayload {
    def validate: ValidatedNel[Failure, ReturnLineItemPayload] =
      greaterThan(amount, 0L, "Amount").map(_ ⇒ this)
  }

  /* Payment payloads */

  case class ReturnPaymentsPayload(payments: Map[PaymentMethod.Type, Long])
      extends Validation[ReturnPaymentsPayload] {
    def validate: ValidatedNel[Failure, ReturnPaymentsPayload] =
      payments
        .collect {
          case (paymentType, amount) if amount <= 0 ⇒
            greaterThanOrEqual(amount, 0L, s"$paymentType amount")
        }
        .fold(Validation.ok)(_ |+| _)
        .map(_ ⇒ this)
  }

  case class ReturnPaymentPayload(amount: Long) extends Validation[ReturnPaymentPayload] {
    def validate: ValidatedNel[Failure, ReturnPaymentPayload] =
      greaterThan(amount, 0L, "Amount").map(_ ⇒ this)
  }

  /* Misc */

  case class ReturnMessageToCustomerPayload(message: String)
      extends Validation[ReturnMessageToCustomerPayload] {

    def validate: ValidatedNel[Failure, ReturnMessageToCustomerPayload] =
      (greaterThanOrEqual(message.length, 0, "Message length") |+| lesserThanOrEqual(
        message.length,
        Return.messageToAccountMaxLength,
        "Message length")).map {
        case _ ⇒ this
      }
  }

  case class ReturnReasonPayload(name: String) extends Validation[ReturnReasonPayload] {
    val reasonNameMaxLength = 255

    def validate: ValidatedNel[Failure, ReturnReasonPayload] = {
      val clue = "Reason name length"
      (greaterThan(name.length, 0, clue) |+| lesserThanOrEqual(name.length, reasonNameMaxLength, clue))
        .map(_ ⇒ this)
    }
  }
}
