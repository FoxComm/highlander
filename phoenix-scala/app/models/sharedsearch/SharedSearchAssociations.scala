package models.sharedsearch

import java.time.Instant

import models.cord.Carts
import models.sharedsearch.SharedSearches.scope._
import models.account._
import shapeless._
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.db._

case class SharedSearchAssociation(id: Int = 0,
                                   sharedSearchId: Int,
                                   storeAdminId: Int,
                                   createdAt: Instant = Instant.now)
    extends FoxModel[SharedSearchAssociation]

object SharedSearchAssociation {
  def build(search: SharedSearch, admin: User): SharedSearchAssociation = {
    SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = admin.accountId)
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
  def sharedSearch = foreignKey(SharedSearches.tableName, sharedSearchId, Carts)(_.id)
  def storeAdmin   = foreignKey(Users.tableName, storeAdminId, Users)(_.accountId)
}

object SharedSearchAssociations
    extends FoxTableQuery[SharedSearchAssociation, SharedSearchAssociations](
      new SharedSearchAssociations(_))
    with ReturningId[SharedSearchAssociation, SharedSearchAssociations] {

  val returningLens: Lens[SharedSearchAssociation, Int] = lens[SharedSearchAssociation].id

  def byStoreAdmin(admin: User): QuerySeq = filter(_.storeAdminId === admin.accountId)

  def associatedWith(admin: User, scope: SharedSearch.Scope): SharedSearches.QuerySeq =
    for {
      associations ← byStoreAdmin(admin).map(_.sharedSearchId)
      searches     ← SharedSearches.notDeleted.filter(_.id === associations).filter(_.scope === scope)
    } yield searches

  def bySharedSearch(search: SharedSearch): QuerySeq = filter(_.sharedSearchId === search.id)

  def associatedAdmins(search: SharedSearch): Users.QuerySeq = {
    for {
      associations ← bySharedSearch(search).map(_.storeAdminId)
      admins       ← Users.filter(_.accountId === associations)
    } yield admins
  }
}
