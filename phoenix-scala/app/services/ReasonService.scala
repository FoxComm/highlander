package services

import failures.InvalidReasonTypeFailure
import models.Reason.ReasonType
import models.{Reason, Reasons}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object ReasonService {

  def listReasonsByType(reasonType: String)(implicit ec: EC, db: DB): DbResultT[Seq[Reason]] =
    ReasonType.read(reasonType) match {
      case Some(rt) ⇒
        val query = Reasons.filter(_.reasonType === rt)
        DbResultT.fromDbio(query.result)
      case _ ⇒
        DbResultT.failures(InvalidReasonTypeFailure(reasonType).single)
    }
}
