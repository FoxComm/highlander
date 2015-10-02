package responses

import cats.data.Xor
import services.Failures
import scala.collection.immutable.Seq

object StoreCreditBulkUpdateResponse {
  final case class ItemResult(
    id: Int,
    success: Boolean = false,
    storeCredit: Option[StoreCreditResponse.Root] = None,
    errors: Option[Failures] = None)

  final case class BulkResponse(itemResults: Seq[ItemResult])

  def buildItemResult(id: Int, entity: Failures Xor StoreCreditResponse.Root): ItemResult = {
    entity match {
      case Xor.Left(errors) ⇒ ItemResult(id = id, errors = Some(errors))
      case Xor.Right(sc)    ⇒ ItemResult(id = id, success = true, storeCredit = Some(sc))
    }
  }

  def buildBulkResponse(itemResults: Seq[ItemResult]): BulkResponse = BulkResponse(itemResults = itemResults)
}

