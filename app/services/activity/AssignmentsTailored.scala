package services.activity

import responses.{CustomerResponse, GiftCardResponse, StoreAdminResponse}
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

  /* GiftCards */
  final case class AssignedToGiftCard(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.RootSimple,
    assignees: Seq[StoreAdminResponse.Root])
    extends ActivityBase[AssignedToGiftCard]

  final case class UnassignedFromGiftCard(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.RootSimple,
    assignee: StoreAdminResponse.Root)
    extends ActivityBase[UnassignedFromGiftCard]

  final case class BulkAssignedToGiftCards(admin: StoreAdminResponse.Root, assignee: StoreAdminResponse.Root,
    giftCardCodes: Seq[String])
    extends ActivityBase[BulkAssignedToGiftCards]

  final case class BulkUnassignedFromGiftCards(admin: StoreAdminResponse.Root, assignee: StoreAdminResponse.Root,
    giftCardCodes: Seq[String])
    extends ActivityBase[BulkUnassignedFromGiftCards]
}
