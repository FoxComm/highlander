package services.activity

import models.Assignment._
import responses.StoreAdminResponse.{Root ⇒ AdminRoot}

object AssignmentsTailored {

  final case class Assigned[T](admin: AdminRoot, entity: T, assignees: Seq[AdminRoot], assignmentType: AssignmentType,
    referenceType: ReferenceType)
    extends ActivityBase[Assigned[T]]

  final case class Unassigned[T](admin: AdminRoot, entity: T, assignee: AdminRoot, assignmentType: AssignmentType,
    referenceType: ReferenceType)
    extends ActivityBase[Unassigned[T]]

  final case class BulkAssigned[K](admin: AdminRoot, assignee: AdminRoot, entityIds: Seq[K],
    assignmentType: AssignmentType, referenceType: ReferenceType)
    extends ActivityBase[BulkAssigned[K]]

  final case class BulkUnassigned[K](admin: AdminRoot, assignee: AdminRoot, entityIds: Seq[K],
    assignmentType: AssignmentType, referenceType: ReferenceType)
    extends ActivityBase[BulkUnassigned[K]]
}
