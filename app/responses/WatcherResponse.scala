package responses

import java.time.Instant

import models.StoreAdmin
import models.customer.CustomerWatcher
import models.order.OrderWatcher

object WatcherResponse {

  final case class Root(
    watcher: StoreAdminResponse.Root,
    createdAt: Instant
  ) extends ResponseItem

  def build(watcher: OrderWatcher, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), watcher.createdAt)

  def buildForCustomer(watcher: CustomerWatcher, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), watcher.createdAt)
}
