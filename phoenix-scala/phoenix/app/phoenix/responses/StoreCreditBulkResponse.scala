package phoenix.responses

import failures.Failures

object StoreCreditBulkResponse {
  case class ItemResult(id: Int,
                        success: Boolean = false,
                        storeCredit: Option[StoreCreditResponse.Root] = None,
                        errors: Option[List[String]] = None)
      extends ResponseItem

  def buildItemResult(id: Int, result: Either[Failures, StoreCreditResponse.Root]): ItemResult = {
    result.fold(errors ⇒ ItemResult(id = id, errors = Some(errors.flatten)),
                sc ⇒ ItemResult(id = id, success = true, storeCredit = Some(sc)))
  }
}
