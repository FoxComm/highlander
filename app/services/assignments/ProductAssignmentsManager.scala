package services.assignments

import models.Assignment
import models.product._
import responses.ProductResponses.ProductHeadResponse._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object ProductAssignmentsManager extends AssignmentsManager[Int, Product] {

  val assignmentType: Assignment.AssignmentType = Assignment.Assignee
  val referenceType: Assignment.ReferenceType = Assignment.Product
  val notifyDimension: String = models.activity.Dimension.product

  def buildResponse(model: Product): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Product] =
    Products.mustFindById404(id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Product]] =
    Products.filter(_.id.inSetBind(ids)).result.toXor
}
