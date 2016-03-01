package models.order

import java.time.Instant

import models.{StoreAdmin, StoreAdmins, javaTimeSlickMapper}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}
import utils.aliases._

final case class OrderWatcher(id: Int = 0, orderId: Int, watcherId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[OrderWatcher]

object OrderWatcher

class OrderWatchers(tag: Tag) extends GenericTable.TableWithId[OrderWatcher](tag, "order_watchers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
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

  def watchingTo(admin: StoreAdmin)(implicit ec: EC): Orders.QuerySeq = {
    for {
      orderWatchers ← byWatcher(admin).map(_.orderId)
      orders        ← Orders.filter(_.id === orderWatchers)
    } yield orders
  }

  def byOrder(order: Order): QuerySeq = filter(_.orderId === order.id)

  def watchersFor(order: Order)(implicit ec: EC): StoreAdmins.QuerySeq = {
    for {
      orderWatchers ← byOrder(order).map(_.watcherId)
      admins        ← StoreAdmins.filter(_.id === orderWatchers)
    } yield admins
  }
}
