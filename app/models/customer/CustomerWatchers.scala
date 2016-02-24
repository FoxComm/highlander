package models.customer

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{StoreAdmin, StoreAdmins, javaTimeSlickMapper}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class CustomerWatcher(id: Int = 0, customerId: Int, watcherId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[CustomerWatcher]

object CustomerWatcher

class CustomerWatchers(tag: Tag) extends GenericTable.TableWithId[CustomerWatcher](tag, "customer_watchers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Int]("customer_id")
  def watcherId = column[Int]("watcher_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, customerId, watcherId, createdAt) <> ((CustomerWatcher.apply _).tupled, CustomerWatcher.unapply)
  def order = foreignKey(Customers.tableName, customerId, Customers)(_.id)
  def watcher = foreignKey(StoreAdmins.tableName, watcherId, StoreAdmins)(_.id)
}

object CustomerWatchers extends TableQueryWithId[CustomerWatcher, CustomerWatchers](
  idLens = GenLens[CustomerWatcher](_.id)
)(new CustomerWatchers(_)) {

  def byWatcher(admin: StoreAdmin): QuerySeq = filter(_.watcherId === admin.id)

  def watchingTo(admin: StoreAdmin)(implicit ec: ExecutionContext): Customers.QuerySeq = {
    for {
      watchers  ← byWatcher(admin).map(_.customerId)
      customers ← Customers.filter(_.id === watchers)
    } yield customers
  }

  def byCustomer(order: Customer): QuerySeq = filter(_.customerId === order.id)

  def watchersFor(order: Customer)(implicit ec: ExecutionContext): StoreAdmins.QuerySeq = {
    for {
      watchers ← byCustomer(order).map(_.watcherId)
      admins   ← StoreAdmins.filter(_.id === watchers)
    } yield admins
  }
}
