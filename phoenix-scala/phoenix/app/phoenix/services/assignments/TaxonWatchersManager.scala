package phoenix.services.assignments

import phoenix.models.activity.Dimension
import phoenix.models.taxonomy._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.TaxonResponses.TaxonResponse
import slick.jdbc.PostgresProfile.api._
import phoenix.utils.aliases._
import core.db._

object TaxonWatchersManager extends AssignmentsManager[Int, Taxon] {
  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Taxon
  val notifyDimension = Dimension.taxon
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Taxon): TaxonResponse = TaxonResponse.build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Taxon] =
    Taxons.mustFindByContextAndFormId404(defaultContextId, id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Taxon]] =
    Taxons.filter(_.formId.inSetBind(ids)).result.dbresult
}
