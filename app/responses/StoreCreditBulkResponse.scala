package responses

import cats.data.Xor
import services.Failures
import scala.collection.immutable.Seq

object StoreCreditBulkResponse {
  final case class ItemResult(
    id: Int,
    success: Boolean = false,
    storeCredit: Option[StoreCreditResponse.Root] = None,
    errors: Option[Failures] = None)

  def buildItemResult(id: Int, result: Failures Xor StoreCreditResponse.Root): ItemResult = {
    result.fold(errors ⇒ ItemResult(id = id, errors = Some(errors)),
      sc ⇒ ItemResult(id = id, success = true, storeCredit = Some(sc)))
  }
}

