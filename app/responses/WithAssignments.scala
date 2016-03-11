package responses

import java.time.Instant

import models.{Assignment, Assignments, StoreAdmins}
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._
import utils.aliases._

trait WithAssignments[T <: ResponseItem] {

  type TableTuple = (Assignments#TableElementType, StoreAdmins#TableElementType)
  type DBIOActionTupleSeq = DBIOAction[Seq[TableTuple], NoStream, Effect.Read with Effect.Read]

  final case class Root(entity: T, assignments: Seq[AssignmentResponse])

  final case class AssignmentResponse(assignee: StoreAdminResponse.Root, createdAt: Instant)

  def withAssignments(referenceType: Assignment.ReferenceType, referenceId: Int)
    (implicit ec: EC, db: DB): DBIOActionTupleSeq = for {

    assignments ← Assignments.filter(_.referenceId === referenceId).filter(_.referenceType === referenceType).result
    admins ← StoreAdmins.filter(_.id.inSetBind(assignments.map(_.storeAdminId))).result
  } yield assignments.zip(admins)

  /*
  def buildDbio(root: T, referenceType: Assignment.ReferenceType, referenceId: Int)
    (implicit ec: EC, db: DB): DBIO[Root] = {
      withAssignments(referenceType, referenceId).map {
        _.map { case (assignment, admin) ⇒
          AssignmentResponse(assignee = StoreAdminResponse.build(admin), createdAt = assignment.createdAt)
        }
      }
    }
  }
  */
}