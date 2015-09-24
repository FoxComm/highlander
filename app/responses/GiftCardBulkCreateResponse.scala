package responses

import GiftCardResponse._

object GiftCardBulkCreateResponse {
  final case class BulkResponses(responses: Seq[BulkResponse])

  final case class BulkResponse(
    success: Boolean,
    `object`: Option[GiftCardResponse.Root] = None,
    errors: Option[Seq[String]] = None)

  def buildBulkResponse(giftCard: Option[GiftCardResponse.Root] = None,
    errors: Option[Seq[String]] = None): BulkResponse = {

    (giftCard, errors) match {
      case (Some(gc), _) ⇒ BulkResponse(success = true, `object` = Some(gc))
      case _             ⇒ BulkResponse(success = false, errors = errors)
    }
  }

  def buildBulkResponses(responses: Seq[BulkResponse]): BulkResponses = BulkResponses(responses = responses)
}