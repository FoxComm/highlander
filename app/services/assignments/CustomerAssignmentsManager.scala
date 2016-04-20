package services.assignments

import models.{Assignment, NotificationSubscription}
import models.customer._
import responses.CustomerResponse.{Root, build}
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.aliases._

object CustomerAssignmentsManager extends AssignmentsManager[Int, Customer] {

  val assignmentType  = Assignment.Assignee
  val referenceType   = Assignment.Customer
  val notifyDimension = models.activity.Dimension.customer
  val notifyReason    = NotificationSubscription.Assigned

  def buildResponse(model: Customer): Root = build(model)

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Customer] =
    Customers.mustFindById404(id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Customer]] =
    Customers.filter(_.id.inSetBind(ids)).result.toXor
}
