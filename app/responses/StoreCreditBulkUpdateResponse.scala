package responses

import models.StoreCredit

object StoreCreditBulkUpdateResponse {
  final case class Root(responses: Seq[Response])

  final case class Response(
    id: Int,
    success: Boolean,
    `object`: Option[StoreCreditResponse.Root] = None,
    errors: Option[Seq[String]] = None
  )

  def buildResponse(id: Int, storeCredit: Option[StoreCredit], errors: Option[Seq[String]] = None): Response = {
    (storeCredit, errors) match {
      case (Some(sc), _) ⇒ Response(id = id, success = true, `object` = Some(StoreCreditResponse.build(sc)))
      case _             ⇒ Response(id = id, success = false, errors = errors)
    }
  }

  def build(responses: Seq[Response]): Root = Root(responses = responses)
}

