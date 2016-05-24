package responses

import cats.data.Xor
import failures.Failures

object GiftCardBulkResponse {
  case class ItemResult(code: Option[String] = None,
                        success: Boolean = false,
                        giftCard: Option[GiftCardResponse.Root] = None,
                        errors: Option[List[String]] = None)
      extends ResponseItem

  def buildItemResult(
      result: Failures Xor GiftCardResponse.Root, code: Option[String] = None): ItemResult = {
    result.fold(errors ⇒ ItemResult(code = code, errors = Some(errors.flatten)),
                gc ⇒ ItemResult(code = code, success = true, giftCard = Some(gc)))
  }
}
