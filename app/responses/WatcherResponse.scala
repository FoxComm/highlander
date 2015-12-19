package responses

import java.time.Instant

import models.{StoreAdmin, OrderWatcher}

object WatcherResponse {

  final case class Root(
    watcher: StoreAdminResponse.Root,
    createdAt: Instant
  ) extends ResponseItem

  def build(watcher: OrderWatcher, admin: StoreAdmin): Root =
    Root(StoreAdminResponse.build(admin), watcher.createdAt)
}
