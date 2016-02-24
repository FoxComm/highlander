package models.customer

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{StoreAdmin, StoreAdmins, javaTimeSlickMapper}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class CustomerAssignment(id: Int = 0, customerId: Int, assigneeId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[CustomerAssignment]

object CustomerAssignment

class CustomerAssignments(tag: Tag) extends GenericTable.TableWithId[CustomerAssignment](tag, "customer_assignments") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def assigneeId = column[Int]("assignee_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, customerId, assigneeId, createdAt) <> ((CustomerAssignment.apply _).tupled, CustomerAssignment.unapply)
  def customer = foreignKey(Customers.tableName, customerId, Customers)(_.id)
  def assignee = foreignKey(StoreAdmins.tableName, assigneeId, StoreAdmins)(_.id)
}

object CustomerAssignments extends TableQueryWithId[CustomerAssignment, CustomerAssignments](
  idLens = GenLens[CustomerAssignment](_.id)
)(new CustomerAssignments(_)) {

  def byAssignee(admin: StoreAdmin): QuerySeq = filter(_.assigneeId === admin.id)

  def assignedTo(admin: StoreAdmin)(implicit ec: ExecutionContext): Customers.QuerySeq = {
    for {
      assignees ← byAssignee(admin).map(_.customerId)
      customers ← Customers.filter(_.id === assignees)
    } yield customers
  }

  def byCustomer(customer: Customer): QuerySeq = filter(_.customerId === customer.id)

  def assigneesFor(customer: Customer)(implicit ec: ExecutionContext): StoreAdmins.QuerySeq = {
    for {
      assignees ← byCustomer(customer).map(_.assigneeId)
      admins    ← StoreAdmins.filter(_.id === assignees)
    } yield admins
  }
}
