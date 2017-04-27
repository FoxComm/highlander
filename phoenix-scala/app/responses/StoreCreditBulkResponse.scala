package responses

import failures.Failures
import io.circe.syntax._
import utils.aliases._
import utils.json.codecs._

object StoreCreditBulkResponse {
  case class ItemResult(id: Int,
                        success: Boolean = false,
                        storeCredit: Option[StoreCreditResponse.Root] = None,
                        errors: Option[List[String]] = None)
      extends ResponseItem {
    def json: Json = this.asJson
  }

  def buildItemResult(id: Int, result: Either[Failures, StoreCreditResponse.Root]): ItemResult = {
    result.fold(errors ⇒ ItemResult(id = id, errors = Some(errors.flatten)),
                sc ⇒ ItemResult(id = id, success = true, storeCredit = Some(sc)))
  }
}
