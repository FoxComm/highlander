package services

import failures.InvalidReasonTypeFailure
import models.Reason.ReasonType
import models.rma.{RmaReason, RmaReasons}
import models.{Reason, Reasons}
import responses.TheResponse
import slick.driver.PostgresDriver.api._
import utils.http.CustomDirectives.SortAndPage
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object ReasonService {

  def listReasons(
      implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[Seq[Reason]]] = {
    Reasons.queryAll.result.toTheResponse.run()
  }

  // FIXME: ugly `_ <: Seq` should be just `Seq`
  def listReasonsByType(reasonType: String)(
      implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[_ <: Seq[Reason]]] = {

    val toAdt = ReasonType.read(reasonType)

    val rwm = toAdt match {
      case Some(rt) ⇒
        val query = Reasons.filter(_.reasonType === toAdt)
        Reasons.sortedAndPaged(query).result
      case _ ⇒
        ResultWithMetadata.fromFailures(InvalidReasonTypeFailure(reasonType).single)
    }
    rwm.toTheResponse.run()
  }

  def listRmaReasons(
      implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[Seq[RmaReason]]] = {
    RmaReasons.queryAll.result.toTheResponse.run()
  }
}
