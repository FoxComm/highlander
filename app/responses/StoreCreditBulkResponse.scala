package responses

import cats.data.Xor
import failures.Failures
import scala.collection.immutable.Seq

object StoreCreditBulkResponse {
  case class ItemResult(id: Int,
                        success: Boolean = false,
                        storeCredit: Option[StoreCreditResponse.Root] = None,
                        errors: Option[List[String]] = None)
      extends ResponseItem

  def buildItemResult(id: Int, result: Failures Xor StoreCreditResponse.Root): ItemResult = {
    result.fold(errors ⇒ ItemResult(id = id, errors = Some(errors.flatten)),
                sc ⇒ ItemResult(id = id, success = true, storeCredit = Some(sc)))
  }
}
