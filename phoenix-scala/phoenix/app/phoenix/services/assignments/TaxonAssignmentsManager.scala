package phoenix.services.assignments

import core.db._
import phoenix.models.activity.Dimension
import phoenix.models.taxonomy._
import phoenix.models.{Assignment, NotificationSubscription}
import phoenix.responses.TaxonResponses.TaxonResponse
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object TaxonAssignmentsManager extends AssignmentsManager[Int, Taxon] {
  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Taxon
  val notifyDimension = Dimension.taxon
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Taxon): TaxonResponse = TaxonResponse.build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Taxon] =
    Taxons.mustFindByContextAndFormId404(defaultContextId, id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Taxon]] =
    Taxons.filter(_.formId.inSetBind(ids)).result.dbresult
}
