package services.assignments

import models.Assignment
import models.customer._
import responses.CustomerResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object CustomerWatchersManager extends AssignmentsManager[Int, Customer] {

  val assignmentType: Assignment.AssignmentType = Assignment.Watcher
  val referenceType: Assignment.ReferenceType = Assignment.Customer
  val notifyDimension: String = models.activity.Dimension.customer

  def buildResponse(model: Customer): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Customer] =
    Customers.mustFindById404(id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Customer]] =
    Customers.filter(_.id.inSetBind(ids)).result.toXor
}
