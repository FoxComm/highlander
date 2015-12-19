package models

import java.time.Instant
import scala.concurrent.ExecutionContext

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class RmaAssignment(id: Int = 0, rmaId: Int = 0, assigneeId: Int = 0, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[RmaAssignment]

object RmaAssignment {

}

class RmaAssignments(tag: Tag) extends GenericTable.TableWithId[RmaAssignment](tag, "rma_assignments") {
  def id = column[Int]("id", O.AutoInc)
  def rmaId = column[Int]("rma_id")
  def assigneeId = column[Int]("assignee_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, rmaId, assigneeId, createdAt) <>((RmaAssignment.apply _).tupled, RmaAssignment.unapply)
  def rma = foreignKey(Rmas.tableName, rmaId, Rmas)(_.id)
  def assignee = foreignKey(StoreAdmins.tableName, assigneeId, StoreAdmins)(_.id)
}

object RmaAssignments extends TableQueryWithId[RmaAssignment, RmaAssignments](
  idLens = GenLens[RmaAssignment](_.id)
)(new RmaAssignments(_)) {

  def byAssignee(admin: StoreAdmin): QuerySeq = filter(_.assigneeId === admin.id)

  def assignedTo(admin: StoreAdmin)(implicit ec: ExecutionContext): DBIO[Seq[Rma]] = {
    for {
      rmasAssignees ← byAssignee(admin).result
      rmaIds = rmasAssignees.map(_.rmaId)
      rmas ← Rmas.filter(_.id.inSetBind(rmaIds)).result
    } yield rmas
  }

  def byRma(rma: Rma): QuerySeq = filter(_.rmaId === rma.id)

  def assigneesFor(rma: Rma)(implicit ec: ExecutionContext): DBIO[Seq[StoreAdmin]] = {
    for {
      rmasAssignees ← byRma(rma).result
      adminIds = rmasAssignees.map(_.assigneeId)
      admins ← StoreAdmins.filter(_.id.inSet(adminIds)).result
    } yield admins
  }
}