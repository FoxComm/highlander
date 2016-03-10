package models

import java.time.Instant

import com.pellucid.sealerate
import models.Assignment.{AssignmentType, ReferenceType}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.aliases._
import utils.{ModelWithIdParameter, ADT, GenericTable, TableQueryWithId}

final case class Assignment(id: Int = 0, assignmentType: AssignmentType, storeAdminId: Int, referenceId: Int,
  referenceType: ReferenceType, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[Assignment] {

}

object Assignment {
  sealed trait AssignmentType
  case object Assignee extends AssignmentType
  case object Watcher extends AssignmentType

  object AssignmentType extends ADT[AssignmentType] {
    def types = sealerate.values[AssignmentType]
  }

  implicit val assignemtTypeColumnType: JdbcType[AssignmentType] with BaseTypedType[AssignmentType] =
    AssignmentType.slickColumn

  sealed trait ReferenceType
  case object Order extends ReferenceType
  case object GiftCard extends ReferenceType
  case object Customer extends ReferenceType
  case object Rma extends ReferenceType
  case object Sku extends ReferenceType
  case object Product extends ReferenceType

  object ReferenceType extends ADT[ReferenceType] {
    def types = sealerate.values[ReferenceType]
  }

  implicit val refTypeColumnType: JdbcType[ReferenceType] with BaseTypedType[ReferenceType] =
    ReferenceType.slickColumn
}

class Assignments(tag: Tag) extends GenericTable.TableWithId[Assignment](tag, "assignments")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def assignmentType = column[AssignmentType]("assignment_type")
  def storeAdminId = column[Int]("store_admin_id")
  def referenceId = column[Int]("reference_id")
  def referenceType = column[ReferenceType]("reference_type")
  def createdAt = column[Instant]("created_at")

  def * = (id, assignmentType, storeAdminId, referenceId,
    referenceType, createdAt) <> ((Assignment.apply _).tupled, Assignment.unapply)

  def storeAdmin = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object Assignments extends TableQueryWithId[Assignment, Assignments](
  idLens = GenLens[Assignment](_.id)
)(new Assignments(_)) {

  def filterByIdAndAdminId(id: Int, adminId: Int): QuerySeq =
    filter(_.id === id).filter(_.storeAdminId === adminId)

  def byStoreAdmin(admin: StoreAdmin): QuerySeq = filter(_.storeAdminId === admin.id)

  def assignedTo(admin: StoreAdmin, table: TableQueryWithId)(implicit ec: EC): QuerySeq = {
    for {
      assignees ← byStoreAdmin(admin).map(_.referenceId)
      records   ← table.filter(_.id === assignees)
    } yield records
  }

  def byEntity[T <: ModelWithIdParameter[T]](model: T): QuerySeq = filter(_.referenceId === model.id)

  def assigneesFor[T](entity: T)(implicit ec: EC): StoreAdmins.QuerySeq = {
    for {
      assignees ← byEntity(entity).map(_.storeAdminId)
      admins    ← StoreAdmins.filter(_.id === assignees)
    } yield admins
  }
}
