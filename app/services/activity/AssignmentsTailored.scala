package services.activity

import responses.{FullOrder, StoreAdminResponse}

object AssignmentsTailored {
  final case class AssignedToOrder(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    assignees: Seq[StoreAdminResponse.Root])
    extends ActivityBase[AssignedToOrder]

  final case class UnassignedFromOrder(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    assignee: StoreAdminResponse.Root)
    extends ActivityBase[UnassignedFromOrder]

  final case class BulkAssignedToOrders(admin: StoreAdminResponse.Root, assignee: StoreAdminResponse.Root,
    orderRefNums: Seq[String])
    extends ActivityBase[BulkAssignedToOrders]

  final case class BulkUnassignedFromOrders(admin: StoreAdminResponse.Root, assignee: StoreAdminResponse.Root,
    orderRefNums: Seq[String])
    extends ActivityBase[BulkUnassignedFromOrders]
}
