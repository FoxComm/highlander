package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

import scala.concurrent.ExecutionContext

final case class OrderAssignment(id: Int = 0, orderId: Int = 0, assigneeId: Int = 0, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[OrderAssignment]

object OrderAssignment

class OrderAssignments(tag: Tag) extends GenericTable.TableWithId[OrderAssignment](tag, "order_assignments") {
  def id = column[Int]("id", O.AutoInc)
  def orderId = column[Int]("order_id")
  def assigneeId = column[Int]("assignee_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, orderId, assigneeId, createdAt) <> ((OrderAssignment.apply _).tupled, OrderAssignment.unapply)
  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def assignee = foreignKey(StoreAdmins.tableName, assigneeId, StoreAdmins)(_.id)
}

object OrderAssignments extends TableQueryWithId[OrderAssignment, OrderAssignments](
  idLens = GenLens[OrderAssignment](_.id)
)(new OrderAssignments(_)) {

  def byAssignee(admin: StoreAdmin): QuerySeq = filter(_.assigneeId === admin.id)

  def assignedTo(admin: StoreAdmin)(implicit ec: ExecutionContext): Orders.QuerySeq = {
    for {
      ordersAssignees ← byAssignee(admin).map(_.orderId)
      orders          ← Orders.filter(_.id === ordersAssignees)
    } yield orders
  }

  def byOrder(order: Order): QuerySeq = filter(_.orderId === order.id)

  def assigneesFor(order: Order)(implicit ec: ExecutionContext): StoreAdmins.QuerySeq = {
    for {
      ordersAssignees ← byOrder(order).map(_.assigneeId)
      admins          ← StoreAdmins.filter(_.id === ordersAssignees)
    } yield admins
  }
}
