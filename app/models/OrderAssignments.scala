package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

import scala.concurrent.ExecutionContext

final case class OrderAssignment(id: Int = 0, orderId: Int = 0, assigneeId: Int = 0, assignedAt: Instant = Instant.now)
  extends ModelWithIdParameter[OrderAssignment]

object OrderAssignment

class OrderAssignments(tag: Tag) extends GenericTable.TableWithId[OrderAssignment](tag, "order_assignments") {
  def id = column[Int]("id", O.AutoInc)
  def orderId = column[Int]("order_id")
  def assigneeId = column[Int]("assignee_id")

  def assignedAt = column[Instant]("assigned_at")

  def * = (id, orderId, assigneeId, assignedAt) <>((OrderAssignment.apply _).tupled, OrderAssignment.unapply)
  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def assignee = foreignKey(StoreAdmins.tableName, assigneeId, StoreAdmins)(_.id)
}

object OrderAssignments extends TableQueryWithId[OrderAssignment, OrderAssignments](
  idLens = GenLens[OrderAssignment](_.id)
)(new OrderAssignments(_)) {

  def byAssignee(admin: StoreAdmin): QuerySeq = filter(_.assigneeId === admin.id)

  def assignedTo(admin: StoreAdmin)(implicit ec: ExecutionContext): DBIO[Seq[Order]] = {
    for {
      ordersAssignees ← byAssignee(admin).result
      orderIds = ordersAssignees.map(_.orderId)
      orders ← Orders.filter(_.id.inSetBind(orderIds)).result
    } yield orders
  }

  def byOrder(order: Order): QuerySeq = filter(_.orderId === order.id)

  def assigneesFor(order: Order)(implicit ec: ExecutionContext): DBIO[Seq[StoreAdmin]] = {
    for {
      ordersAssignees ← byOrder(order).result
      adminIds = ordersAssignees.map(_.assigneeId)
      admins ← StoreAdmins.filter(_.id.inSet(adminIds)).result
    } yield admins
  }
}
