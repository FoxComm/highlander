package responses

import models.GiftCard

object GiftCardBulkUpdateResponse {
  final case class Responses(responses: Seq[Response])

  final case class Response(
    code: String,
    success: Boolean,
    giftCard: Option[GiftCardResponse.Root] = None,
    errors: Option[Seq[String]] = None)

  def buildResponse(code: String, giftCard: Option[GiftCardResponse.Root] = None,
    errors: Option[Seq[String]] = None): Response = {

    (giftCard, errors) match {
      case (Some(gc), _) ⇒ Response(code = code, success = true, giftCard = Some(gc))
      case _             ⇒ Response(code = code, success = false, errors = errors)
    }
  }

  def buildResponses(responses: Seq[Response]): Responses = Responses(responses = responses)
}