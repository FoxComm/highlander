package services.returns

import failures.NotFoundFailure404
import models.returns._
import payloads.ReturnPayloads.ReturnReasonPayload
import responses.ReturnReasonsResponse.{buildResponse, _}
import slick.driver.PostgresDriver.api._
import utils.aliases.{DB, EC}
import utils.db.{*, DbResultT, _}

object ReturnReasonsManager {

  def reasonsList(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      reasons  ← * <~ ReturnReasons.result
      response ← * <~ reasons.map(buildResponse)
    } yield response

  def addReason(reasonPayload: ReturnReasonPayload)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      _        ← * <~ reasonPayload.validate
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
