package phoenix.models.cord

import cats.data.{NonEmptyList, Validated, ValidatedNel}
import cats.implicits._
import core.failures.Failure
import core.utils.Validation
import core.utils.Validation._
import phoenix.models.payment.giftcard.GiftCard.giftCardCodeRegex
import phoenix.payloads.AddressPayloads.CreateAddressPayload

package object lineitems {

  case class LineItemAttributes(giftCard: Option[GiftCardLineItemAttributes] = None,
                                subscription: Option[CreateAddressPayload] = None)
      extends Validation[LineItemAttributes] {

    override def validate: ValidatedNel[Failure, LineItemAttributes] = {
      val giftCardOk     = giftCard.fold(ok)(_.validate.map(_ ⇒ Unit))
      val subscriptionOk = subscription.fold(ok)(_.validate.map(_ ⇒ Unit))

      (giftCardOk |@| subscriptionOk).map { case _ ⇒ this }
    }
  }

  /*
   * GC line item attrs receive `code` when all of the following fulfill:
   * 1. cart becomes order
   * 2. order moves to Shipped
   * 3. MWH creates gift cards
   * Hence, order has no `code` for some period of time, so we can't guarantee presence of `code`.
   */
  case class GiftCardLineItemAttributes(senderName: String,
                                        recipientName: String,
                                        recipientEmail: String,
                                        message: Option[String] = None,
                                        code: Option[String] = None)
      extends Validation[GiftCardLineItemAttributes] {

    override def validate: Validated[NonEmptyList[Failure], GiftCardLineItemAttributes] = {
      val senderNameOk     = notEmpty(senderName, "sender name")
      val recipientNameOk  = notEmpty(recipientName, "recipient name")
      val messageOk        = message.fold(ok)(msg ⇒ notEmpty(msg, "message"))
      val recipientEmailOk = emailish(recipientEmail, "recipient email")
      val codeOk = code.fold(ok) { gcCode ⇒
        validExpr(gcCode.matches(giftCardCodeRegex.regex), "code must be a gift card code")
      }

      (senderNameOk |@| recipientNameOk |@| messageOk |@| recipientEmailOk |@| codeOk).map {
        case _ ⇒ this
      }
    }
  }

}
