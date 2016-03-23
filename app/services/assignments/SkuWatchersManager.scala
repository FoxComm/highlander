package services.assignments

import models.Assignment
import models.inventory.{Sku, Skus}
import responses.SkuResponses.SkuHeadResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object SkuWatchersManager extends AssignmentsManager[String, Sku] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Watcher
  def referenceType(): Assignment.ReferenceType = Assignment.Sku
  def notifyDimension(): String = models.activity.Dimension.sku

  def buildResponse(model: Sku): Root = build(model)

  def fetchEntity(code: String)(implicit ec: EC, db: DB, ac: AC): DbResult[Sku] =
    Skus.mustFindByCode(code)

  def fetchSequence(codes: Seq[String])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Sku]] =
    Skus.filter(_.code.inSetBind(codes)).result.toXor
}
