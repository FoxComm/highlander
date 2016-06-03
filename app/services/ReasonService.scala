package services

import failures.InvalidReasonTypeFailure
import models.Reason.ReasonType
import models.{Reason, Reasons}
import responses.TheResponse
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._
import utils.http.CustomDirectives.SortAndPage

object ReasonService {

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
}
