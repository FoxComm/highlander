package responses

import cats.data.Xor
import services.Failures

object StoreCreditBulkUpdateResponse {
  final case class Response(
    id: Int,
    success: Boolean = false,
    storeCredit: Option[StoreCreditResponse.Root] = None,
    errors: Option[Seq[String]] = None)

  final case class Responses(responses: Seq[Response])

  def buildResponse(id: Int, entity: Failures Xor StoreCreditResponse.Root): Response = {
    entity match {
      case Xor.Left(errors) ⇒ Response(id = id, errors = Some(errors.map(_.description.mkString)))
      case Xor.Right(sc)    ⇒ Response(id = id, success = true, storeCredit = Some(sc))
    }
  }

  def buildResponses(responses: Seq[Response]): Responses = Responses(responses = responses)
}

