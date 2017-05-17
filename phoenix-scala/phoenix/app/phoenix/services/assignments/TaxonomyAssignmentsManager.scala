package phoenix.services.assignments

import phoenix.models.activity.Dimension
import phoenix.models.taxonomy._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.TaxonomyResponses.TaxonomyResponse._
import slick.jdbc.PostgresProfile.api._
import phoenix.utils.aliases._
import utils.db._

object TaxonomyAssignmentsManager extends AssignmentsManager[Int, Taxonomy] {
  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Taxonomy
  val notifyDimension = Dimension.taxonomy
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Taxonomy): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Taxonomy] =
    Taxonomies.mustFindByContextAndFormId404(defaultContextId, id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Taxonomy]] =
    Taxonomies.filter(_.formId.inSetBind(ids)).result.dbresult
}
