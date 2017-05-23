package phoenix.services.activity

import phoenix.models.Assignment._
import phoenix.responses.UserResponse.{Root ⇒ UserRoot}

object AssignmentsTailored {

  case class Assigned[T](admin: UserRoot,
                         entity: T,
                         assignees: Seq[UserRoot],
                         assignmentType: AssignmentType,
                         referenceType: ReferenceType)
      extends ActivityBase[Assigned[T]]

  case class Unassigned[T](admin: UserRoot,
                           entity: T,
                           assignee: UserRoot,
                           assignmentType: AssignmentType,
                           referenceType: ReferenceType)
      extends ActivityBase[Unassigned[T]]

  case class BulkAssigned(admin: UserRoot,
                          assignee: UserRoot,
                          entityIds: Seq[String],
                          assignmentType: AssignmentType,
                          referenceType: ReferenceType)
      extends ActivityBase[BulkAssigned]

  case class BulkUnassigned(admin: UserRoot,
                            assignee: UserRoot,
                            entityIds: Seq[String],
                            assignmentType: AssignmentType,
                            referenceType: ReferenceType)
      extends ActivityBase[BulkUnassigned]
}
