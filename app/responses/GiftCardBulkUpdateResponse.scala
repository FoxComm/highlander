package responses

import cats.data.Xor
import services.Failures

object GiftCardBulkUpdateResponse {
  final case class Response(
    code: String,
    success: Boolean = false,
    giftCard: Option[GiftCardResponse.Root] = None,
    errors: Option[Seq[String]] = None)

  final case class Responses(responses: Seq[Response])

  def buildResponse(code: String, result: Failures Xor GiftCardResponse.Root): Response = {
    result match {
      case Xor.Left(errors)  ⇒ Response(code = code, errors = Some(errors.map(_.description.mkString)))
      case Xor.Right(sc)     ⇒ Response(code = code, success = true, giftCard = Some(sc))
    }
  }

  def buildResponses(responses: Seq[Response]): Responses = Responses(responses = responses)
}