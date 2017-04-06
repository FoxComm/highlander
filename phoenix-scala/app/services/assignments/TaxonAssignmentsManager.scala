package services.assignments

import models.taxonomy._
import models.{Assignment, NotificationSubscription}
import responses.TaxonResponses.TaxonResponse._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object TaxonAssignmentsManager extends AssignmentsManager[Int, Taxon] {
  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Taxon
  val notifyDimension = models.activity.Dimension.taxon
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Taxon): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Taxon] =
    Taxons.mustFindByContextAndFormId404(defaultContextId, id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Taxon]] =
    Taxons.filter(_.formId.inSetBind(ids)).result.dbresult
}
