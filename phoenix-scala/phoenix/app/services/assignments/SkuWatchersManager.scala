package services.assignments

import models.{Assignment, NotificationSubscription}
import models.inventory.{Sku, Skus}
import responses.SkuResponses.SkuHeadResponse.{Root, build}
import slick.jdbc.PostgresProfile.api._
import utils.db._
import utils.aliases._

object SkuWatchersManager extends AssignmentsManager[String, Sku] {

  val assignmentType  = Assignment.Watcher
  val referenceType   = Assignment.Sku
  val notifyDimension = models.activity.Dimension.sku
  val notifyReason    = NotificationSubscription.Watching

  def buildResponse(model: Sku): Root = build(model)

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[Sku] =
    Skus.mustFindByCode(code)

  def fetchSequence(codes: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[Sku]] =
    Skus.filter(_.code.inSetBind(codes)).result.dbresult
}
