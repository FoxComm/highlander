package services.assignments

import models.{StoreAdmins, StoreAdmin, Assignment, Assignments}
import responses.TheResponse
import services.Util._
import services._
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.ModelWithIdParameter
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._
import utils.aliases._

trait AssignmentsManager[K, T <: ModelWithIdParameter[T]] {
  // Define this methods in inherit object
  def assignmentType(): Assignment.AssignmentType
  def referenceType(): Assignment.ReferenceType
  def fetchEntity(key: K)(implicit ec: EC, db: DB, ac: AC): DbResult[T]

  // Use this methods wherever you want
  def assign(key: K, assigneeIds: Seq[Int], originator: StoreAdmin)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {

    entity    ← * <~ fetchEntity(key)
    adminIds  ← * <~ StoreAdmins.filter(_.id.inSetBind(assigneeIds)).map(_.id).result
    assignees ← * <~ Assignments.assigneesFor(entity).result.toXor
    _         ← * <~ Assignments.createAll(buildNewAssignments(entity, adminIds, assignees))

    // TODO - TheResponse alternative with embedded assignments
    // ...

    notFoundAdmins  = diffToFailures(assigneeIds, adminIds, StoreAdmin)

  } yield ()).runTxn()

  private def buildNewAssignments(entity: T, adminIds: Seq[Int], assignees: Seq[StoreAdmin]): Seq[Assignment] =
    adminIds.diff(assignees.map(_.id)).map(adminId ⇒ Assignment(assignmentType = assignmentType(),
      storeAdminId = adminId, referenceType = referenceType(), referenceId = entity.id))
}