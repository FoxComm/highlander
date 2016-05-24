package models.sharedsearch

import java.time.Instant

import models.order.Orders
import models.{StoreAdmin, StoreAdmins}
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import models.sharedsearch.SharedSearches.scope._
import utils.db._

case class SharedSearchAssociation(
    id: Int = 0, sharedSearchId: Int, storeAdminId: Int, createdAt: Instant = Instant.now)
    extends FoxModel[SharedSearchAssociation]

object SharedSearchAssociation {
  def build(search: SharedSearch, admin: StoreAdmin): SharedSearchAssociation = {
    SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = admin.id)
  }
}

class SharedSearchAssociations(tag: Tag)
    extends FoxTable[SharedSearchAssociation](tag, "shared_search_associations") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def sharedSearchId = column[Int]("shared_search_id")
  def storeAdminId   = column[Int]("store_admin_id")
  def createdAt      = column[Instant]("created_at")

  def * =
    (id, sharedSearchId, storeAdminId, createdAt) <> ((SharedSearchAssociation.apply _).tupled,
        SharedSearchAssociation.unapply)
  def sharedSearch = foreignKey(SharedSearches.tableName, sharedSearchId, Orders)(_.id)
  def storeAdmin   = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object SharedSearchAssociations
    extends FoxTableQuery[SharedSearchAssociation, SharedSearchAssociations](
        new SharedSearchAssociations(_))
    with ReturningId[SharedSearchAssociation, SharedSearchAssociations] {

  val returningLens: Lens[SharedSearchAssociation, Int] = lens[SharedSearchAssociation].id

  def byStoreAdmin(admin: StoreAdmin): QuerySeq = filter(_.storeAdminId === admin.id)

  def associatedWith(admin: StoreAdmin, scope: SharedSearch.Scope): SharedSearches.QuerySeq =
    for {
      associations ← byStoreAdmin(admin).map(_.sharedSearchId)
      searches     ← SharedSearches.notDeleted.filter(_.id === associations).filter(_.scope === scope)
    } yield searches

  def bySharedSearch(search: SharedSearch): QuerySeq = filter(_.sharedSearchId === search.id)

  def associatedAdmins(search: SharedSearch): StoreAdmins.QuerySeq = {
    for {
      associations ← bySharedSearch(search).map(_.storeAdminId)
      admins       ← StoreAdmins.filter(_.id === associations)
    } yield admins
  }
}
