package services.activity

import responses.{CustomerResponse, GiftCardResponse, StoreAdminResponse}
import responses.order.FullOrder

object WatchersTailored {
  /* Order Watchers */
  final case class AddedWatchersToOrder(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    watchers: Seq[StoreAdminResponse.Root])
    extends ActivityBase[AddedWatchersToOrder]

  final case class RemovedWatcherFromOrder(admin: StoreAdminResponse.Root, order: FullOrder.Root,
    watcher: StoreAdminResponse.Root)
    extends ActivityBase[RemovedWatcherFromOrder]

  final case class BulkAddedWatcherToOrders(admin: StoreAdminResponse.Root, watcher: StoreAdminResponse.Root,
    orderRefNums: Seq[String])
    extends ActivityBase[BulkAddedWatcherToOrders]

  final case class BulkRemovedWatcherFromOrders(admin: StoreAdminResponse.Root, watcher: StoreAdminResponse.Root,
    orderRefNums: Seq[String])
    extends ActivityBase[BulkRemovedWatcherFromOrders]

  /* Customer Watchers */
  final case class AddedWatchersToCustomer(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
    watchers: Seq[StoreAdminResponse.Root])
    extends ActivityBase[AddedWatchersToCustomer]

  final case class RemovedWatcherFromCustomer(admin: StoreAdminResponse.Root, customer: CustomerResponse.Root,
    watcher: StoreAdminResponse.Root)
    extends ActivityBase[RemovedWatcherFromCustomer]

  final case class BulkAddedWatcherToCustomers(admin: StoreAdminResponse.Root, watcher: StoreAdminResponse.Root,
    customerIds: Seq[Int])
    extends ActivityBase[BulkAddedWatcherToCustomers]

  final case class BulkRemovedWatcherFromCustomers(admin: StoreAdminResponse.Root, watcher: StoreAdminResponse.Root,
    customerIds: Seq[Int])
    extends ActivityBase[BulkRemovedWatcherFromCustomers]

  /* Gift Card Watchers */
  final case class AddedWatchersToGiftCard(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.RootSimple,
    watchers: Seq[StoreAdminResponse.Root])
    extends ActivityBase[AddedWatchersToGiftCard]

  final case class RemovedWatcherFromGiftCard(admin: StoreAdminResponse.Root, giftCard: GiftCardResponse.RootSimple,
    watcher: StoreAdminResponse.Root)
    extends ActivityBase[RemovedWatcherFromGiftCard]

  final case class BulkAddedWatcherToGiftCards(admin: StoreAdminResponse.Root, watcher: StoreAdminResponse.Root,
    giftCardCodes: Seq[String])
    extends ActivityBase[BulkAddedWatcherToGiftCards]

  final case class BulkRemovedWatcherFromGiftCards(admin: StoreAdminResponse.Root, watcher: StoreAdminResponse.Root,
    giftCardCodes: Seq[String])
    extends ActivityBase[BulkRemovedWatcherFromGiftCards]
}
