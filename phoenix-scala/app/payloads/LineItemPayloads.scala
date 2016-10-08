package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import utils.Money._
import utils.Validation
import utils.Validation._
import utils.aliases.Json

object LineItemPayloads {
  case class GiftCardRecipient(name: String, email: String, message: String)
      extends Validation[GiftCardRecipient] {
    def validate: ValidatedNel[Failure, GiftCardRecipient] = {
      import Validation._

      (validExpr(name.isEmpty, "recepient name must be given") |@|
            validExpr(email.isEmpty && message.length < 500 && message.length > 0,
                      "email must be given") |@|
            validExpr(message.length > 0, "message  cannot be empty") |@|
            validExpr(message.length < 500, "message can't have more than 500 characters")).map {
        case _ ⇒ this
      }
    }
  }

  case class UpdateLineItemsPayload(sku: String,
                                    quantity: Int,
                                    giftCard: Option[GiftCardRecipient])

}
