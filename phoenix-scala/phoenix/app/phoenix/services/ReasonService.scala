package phoenix.services

import core.db._
import phoenix.failures.InvalidReasonTypeFailure
import phoenix.models.Reason.ReasonType
import phoenix.models.{Reason, Reasons}
import slick.jdbc.PostgresProfile.api._

object ReasonService {

  def listReasonsByType(reasonType: String)(implicit ec: EC, db: DB): DbResultT[Seq[Reason]] =
    ReasonType.read(reasonType) match {
      case Some(rt) ⇒
        val query = Reasons.filter(_.reasonType === rt)
        DbResultT.fromF(query.result)
      case _ ⇒
        DbResultT.failures(InvalidReasonTypeFailure(reasonType).single)
    }
}
