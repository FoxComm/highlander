package phoenix.responses.giftcards

import core.failures.Failures
import phoenix.responses.ResponseItem

case class GiftCardBulkResponse(code: Option[String] = None,
                                success: Boolean = false,
                                giftCard: Option[GiftCardResponse] = None,
                                errors: Option[List[String]] = None)
    extends ResponseItem

object GiftCardBulkResponse {

  def build(result: Either[Failures, GiftCardResponse], code: Option[String] = None): GiftCardBulkResponse =
    result.fold(errors ⇒ GiftCardBulkResponse(code = code, errors = Some(errors.flatten)),
                gc ⇒ GiftCardBulkResponse(code = code, success = true, giftCard = Some(gc)))
}
