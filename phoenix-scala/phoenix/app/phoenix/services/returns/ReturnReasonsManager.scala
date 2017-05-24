package phoenix.services.returns

import core.db._
import core.failures.NotFoundFailure404
import phoenix.models.returns._
import phoenix.payloads.ReturnPayloads.ReturnReasonPayload
import phoenix.responses.ReturnReasonsResponse._
import phoenix.utils.aliases.{DB, EC}
import slick.jdbc.PostgresProfile.api._

object ReturnReasonsManager {

  def reasonsList(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      reasons  ← * <~ ReturnReasons.result
      response ← * <~ reasons.map(buildResponse)
    } yield response

  def addReason(reasonPayload: ReturnReasonPayload)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      reason   ← * <~ addProcessing(reasonPayload)
      response ← * <~ buildResponse(reason)
    } yield response

  private def addProcessing(reasonPayload: ReturnReasonPayload)(implicit ec: EC,
                                                                db: DB): DbResultT[ReturnReason] =
    ReturnReasons.create(ReturnReason(name = reasonPayload.name))

  def deleteReason(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      result ← * <~ ReturnReasons
                .deleteById(id, DbResultT.unit, i ⇒ NotFoundFailure404(ReturnReasons, i))
    } yield result

}
