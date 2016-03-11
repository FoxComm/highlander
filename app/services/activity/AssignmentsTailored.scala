package services.activity

import responses.StoreAdminResponse.{Root ⇒ AdminRoot}

object AssignmentsTailored {

  final case class Assigned[T](admin: AdminRoot, entity: T, assignees: Seq[AdminRoot])
    extends ActivityBase[Assigned[T]]

  final case class Unassigned[T](admin: AdminRoot, entity: T, assignee: AdminRoot)
    extends ActivityBase[Unassigned[T]]

  final case class BulkAssigned[K](admin: AdminRoot, assignee: AdminRoot, entityIds: Seq[K])
    extends ActivityBase[BulkAssigned[K]]

  final case class BulkUnassigned[K](admin: AdminRoot, assignee: AdminRoot, entityIds: Seq[K])
    extends ActivityBase[BulkUnassigned[K]]
}
