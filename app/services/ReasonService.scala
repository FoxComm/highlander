package services

import models.Reason.ReasonType
import models.rma.{RmaReasons, RmaReason}
import models.{Reasons, Reason}
import responses.TheResponse
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._
import utils.DbResultT.implicits._
import utils.aliases._

object ReasonService {

  def listReasons(implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[Seq[Reason]]] = {
    Reasons.queryAll.result.toTheResponse.run()
  }

  // FIXME: ugly `_ <: Seq` should be just `Seq`
  def listReasonsByType(reasonType: String)
    (implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[_ <: Seq[Reason]]] = {

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

  def listRmaReasons(implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[Seq[RmaReason]]] = {
    RmaReasons.queryAll.result.toTheResponse.run()
  }
}
