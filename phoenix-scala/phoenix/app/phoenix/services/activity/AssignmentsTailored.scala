package phoenix.services.activity

import phoenix.models.Assignment._
import phoenix.responses.users.UserResponse

object AssignmentsTailored {

  case class Assigned[T](admin: UserResponse,
                         entity: T,
                         assignees: Seq[UserResponse],
                         assignmentType: AssignmentType,
                         referenceType: ReferenceType)
      extends ActivityBase[Assigned[T]]

  case class Unassigned[T](admin: UserResponse,
                           entity: T,
                           assignee: UserResponse,
                           assignmentType: AssignmentType,
                           referenceType: ReferenceType)
      extends ActivityBase[Unassigned[T]]

  case class BulkAssigned(admin: UserResponse,
                          assignee: UserResponse,
                          entityIds: Seq[String],
                          assignmentType: AssignmentType,
                          referenceType: ReferenceType)
      extends ActivityBase[BulkAssigned]

  case class BulkUnassigned(admin: UserResponse,
                            assignee: UserResponse,
                            entityIds: Seq[String],
                            assignmentType: AssignmentType,
                            referenceType: ReferenceType)
      extends ActivityBase[BulkUnassigned]
}
