package services.assignments

import models.Assignment
import models.customer._
import slick.driver.PostgresDriver.api._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

object CustomerWatchersManager extends AssignmentsManager[Int, Customer] {

  def assignmentType(): Assignment.AssignmentType = Assignment.Watcher
  def referenceType(): Assignment.ReferenceType = Assignment.Customer
  def notifyDimension(): String = models.activity.Dimension.customer

  def fetchEntity(id: Int)(implicit ec: EC, db: DB, ac: AC): DbResult[Customer] =
    Customers.mustFindById404(id)

  def fetchSequence(ids: Seq[Int])(implicit ec: EC, db: DB, ac: AC): DbResult[Seq[Customer]] =
    Customers.filter(_.id.inSetBind(ids)).result.toXor
}
