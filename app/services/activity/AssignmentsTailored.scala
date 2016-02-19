package services.activity

import responses.{CustomerResponse, StoreAdminResponse}
import responses.order.FullOrder

object AssignmentsTailored {
  /* Orders */
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

  /* Customers */
  final case class AssignedToCustomer(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
    assignees: Seq[StoreAdminResponse.Root])
    extends ActivityBase[AssignedToCustomer]

  final case class UnassignedFromCustomer(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
    assignee: StoreAdminResponse.Root)
    extends ActivityBase[UnassignedFromCustomer]

  final case class BulkAssignedToCustomers(admin: StoreAdminResponse.Root, assignee: StoreAdminResponse.Root,
    customerIds: Seq[Int])
    extends ActivityBase[BulkAssignedToCustomers]

  final case class BulkUnassignedFromCustomers(admin: StoreAdminResponse.Root, assignee: StoreAdminResponse.Root,
    customerIds: Seq[Int])
    extends ActivityBase[BulkUnassignedFromCustomers]
}
