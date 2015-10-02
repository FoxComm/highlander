package responses

import cats.data.Xor
import services.Failures
import scala.collection.immutable.Seq

object GiftCardBulkUpdateResponse {
  final case class ItemResult(
    code: String,
    success: Boolean = false,
    giftCard: Option[GiftCardResponse.Root] = None,
    errors: Option[Seq[String]] = None)

  final case class BulkResponse(itemResults: Seq[ItemResult])

  def buildItemResult(code: String, result: Failures Xor GiftCardResponse.Root): ItemResult = {
    result match {
      case Xor.Left(errors)  ⇒ ItemResult(code = code, errors = Some(errors.map(_.description.mkString)))
      case Xor.Right(sc)     ⇒ ItemResult(code = code, success = true, giftCard = Some(sc))
    }
  }

  def buildBulkResponse(itemResults: Seq[ItemResult]): BulkResponse = BulkResponse(itemResults = itemResults)
}