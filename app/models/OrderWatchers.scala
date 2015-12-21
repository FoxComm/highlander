package models

import java.time.Instant

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

import scala.concurrent.ExecutionContext

final case class OrderWatcher(id: Int = 0, orderId: Int = 0, watcherId: Int = 0, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[OrderWatcher]

object OrderWatcher

class OrderWatchers(tag: Tag) extends GenericTable.TableWithId[OrderWatcher](tag, "order_watchers") {
  def id = column[Int]("id", O.AutoInc)
  def orderId = column[Int]("order_id")
  def watcherId = column[Int]("watcher_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, orderId, watcherId, createdAt) <>((OrderWatcher.apply _).tupled, OrderWatcher.unapply)
  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def watcher = foreignKey(StoreAdmins.tableName, watcherId, StoreAdmins)(_.id)
}

object OrderWatchers extends TableQueryWithId[OrderWatcher, OrderWatchers](
  idLens = GenLens[OrderWatcher](_.id)
)(new OrderWatchers(_)) {

  def byWatcher(admin: StoreAdmin): QuerySeq = filter(_.watcherId === admin.id)

  def watchingTo(admin: StoreAdmin)(implicit ec: ExecutionContext): Orders.QuerySeq = {
    for {
      orderWatchers ← byWatcher(admin).map(_.orderId)
      orders        ← Orders.filter(_.id === orderWatchers)
    } yield orders
  }

  def byOrder(order: Order): QuerySeq = filter(_.orderId === order.id)

  def watchersFor(order: Order)(implicit ec: ExecutionContext): StoreAdmins.QuerySeq = {
    for {
      orderWatchers ← byOrder(order).map(_.watcherId)
      admins        ← StoreAdmins.filter(_.id === orderWatchers)
    } yield admins
  }
}
