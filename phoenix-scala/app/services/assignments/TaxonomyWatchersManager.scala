package services.assignments

import models.taxonomy._
import models.{Assignment, NotificationSubscription}
import responses.TaxonomyResponses.TaxonomyResponse._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object TaxonomyWatchersManager extends AssignmentsManager[Int, Taxonomy] {
  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Taxonomy
  val notifyDimension = models.activity.Dimension.taxonomy
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Taxonomy): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResultT[Taxonomy] =
    Taxonomies.mustFindByContextAndFormId404(defaultContextId, id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Taxonomy]] =
    Taxonomies.filter(_.formId.inSetBind(ids)).result.dbresult
}
