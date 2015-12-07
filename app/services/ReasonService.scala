package services

import scala.concurrent.ExecutionContext

import models.Reason.ReasonType
import models.{RmaReasons, RmaReason, Reasons, Reason}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.implicits._

object ReasonService {
  def listReasons(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  ResultWithMetadata[Seq[Reason]] = {
    Reasons.queryAll.result
  }

  def listReasonsByType(reasonType: String)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Reason]] = {

    val toAdt = ReasonType.read(reasonType)

    toAdt match {
      case Some(rt) ⇒
        val query = Reasons.filter(_.reasonType === toAdt)
        Reasons.sortedAndPaged(query).result
      case _ ⇒
        ResultWithMetadata.fromFailures(Failures(InvalidReasonTypeFailure(reasonType)))
    }
  }

  def listRmaReasons(implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage):
  ResultWithMetadata[Seq[RmaReason]] = {
    RmaReasons.queryAll.result
  }
}
