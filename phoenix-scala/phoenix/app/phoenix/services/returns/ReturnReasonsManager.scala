package phoenix.services.returns

import cats.implicits._
import core.db._
import core.failures.NotFoundFailure404
import phoenix.models.returns._
import phoenix.payloads.ReturnPayloads.ReturnReasonPayload
import phoenix.responses.ReturnReasonsResponse
import phoenix.utils.aliases.{DB, EC}
import slick.jdbc.PostgresProfile.api._

object ReturnReasonsManager {

  def reasonsList(implicit ec: EC, db: DB): DbResultT[Seq[ReturnReasonsResponse]] =
    for {
      reasons  ← * <~ ReturnReasons.result
      response ← * <~ reasons.map(ReturnReasonsResponse.build)
    } yield response

  def addReason(reasonPayload: ReturnReasonPayload)(implicit ec: EC,
                                                    db: DB): DbResultT[ReturnReasonsResponse] =
    for {
      reason   ← * <~ addProcessing(reasonPayload)
      response ← * <~ ReturnReasonsResponse.build(reason)
    } yield response

  private def addProcessing(reasonPayload: ReturnReasonPayload)(implicit ec: EC,
                                                                db: DB): DbResultT[ReturnReason] =
    ReturnReasons.create(ReturnReason(name = reasonPayload.name))

  def deleteReason(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      result ← * <~ ReturnReasons
                .deleteById(id, ().pure[DbResultT], NotFoundFailure404(ReturnReasons, _))
    } yield result

}
