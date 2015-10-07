package responses

import cats.data.Xor
import services.Failures

object GiftCardBulkResponse {
  final case class ItemResult(
    code: Option[String] = None,
    success: Boolean = false,
    giftCard: Option[GiftCardResponse.Root] = None,
    errors: Option[Failures] = None)

  def buildItemResult(result: Failures Xor GiftCardResponse.Root, code: Option[String] = None): ItemResult = {
    result.fold(errors ⇒ ItemResult(code = code, errors = Some(errors)),
      gc ⇒ ItemResult(code = code, success = true, giftCard = Some(gc)))
  }
}