package services.activity

import models.Assignment._
import responses.StoreAdminResponse.{Root ⇒ AdminRoot}

object AssignmentsTailored {

  case class Assigned[T](admin: AdminRoot,
                         entity: T,
                         assignees: Seq[AdminRoot],
                         assignmentType: AssignmentType,
                         referenceType: ReferenceType)
      extends ActivityBase[Assigned[T]]

  case class Unassigned[T](admin: AdminRoot,
                           entity: T,
                           assignee: AdminRoot,
                           assignmentType: AssignmentType,
                           referenceType: ReferenceType)
      extends ActivityBase[Unassigned[T]]

  case class BulkAssigned(admin: AdminRoot,
                          assignee: AdminRoot,
                          entityIds: Seq[String],
                          assignmentType: AssignmentType,
                          referenceType: ReferenceType)
      extends ActivityBase[BulkAssigned]

  case class BulkUnassigned(admin: AdminRoot,
                            assignee: AdminRoot,
                            entityIds: Seq[String],
                            assignmentType: AssignmentType,
                            referenceType: ReferenceType)
      extends ActivityBase[BulkUnassigned]
}
