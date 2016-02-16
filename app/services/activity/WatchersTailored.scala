package services.activity

import responses.{FullOrder, StoreAdminResponse}

object WatchersTailored {
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
}
