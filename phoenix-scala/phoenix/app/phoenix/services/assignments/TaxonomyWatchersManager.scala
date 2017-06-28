package phoenix.services.assignments

import core.db._
import phoenix.models.activity.Dimension
import phoenix.models.taxonomy._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.TaxonomyResponses.TaxonomyResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object TaxonomyWatchersManager extends AssignmentsManager[Int, Taxonomy] {
  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Taxonomy
  val notifyDimension = Dimension.taxonomy
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Taxonomy): TaxonomyResponse = TaxonomyResponse.build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Taxonomy] =
    Taxonomies.mustFindByContextAndFormId404(defaultContextId, id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Taxonomy]] =
    Taxonomies.filter(_.formId.inSetBind(ids)).result.dbresult
}
