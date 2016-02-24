package models.sharedsearch

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.order.Orders
import models.{StoreAdmin, StoreAdmins, javaTimeSlickMapper}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class SharedSearchAssociation(id: Int = 0, sharedSearchId: Int, storeAdminId: Int,
  createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[SharedSearchAssociation]

object SharedSearchAssociation {
  def build(search: SharedSearch, admin: StoreAdmin): SharedSearchAssociation = {
    SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = admin.id)
  }
}

class SharedSearchAssociations(tag: Tag)
  extends GenericTable.TableWithId[SharedSearchAssociation](tag, "shared_search_associations") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def sharedSearchId = column[Int]("shared_search_id")
  def storeAdminId = column[Int]("store_admin_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, sharedSearchId, storeAdminId, createdAt) <> ((SharedSearchAssociation.apply _).tupled,
    SharedSearchAssociation.unapply)
  def sharedSearch = foreignKey(SharedSearches.tableName, sharedSearchId, Orders)(_.id)
  def storeAdmin = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object SharedSearchAssociations extends TableQueryWithId[SharedSearchAssociation, SharedSearchAssociations](
  idLens = GenLens[SharedSearchAssociation](_.id)
)(new SharedSearchAssociations(_)) {

  def byStoreAdmin(admin: StoreAdmin): QuerySeq = filter(_.storeAdminId === admin.id)

  def associatedWith(admin: StoreAdmin, scope: Option[String])(implicit ec: ExecutionContext): SharedSearches.QuerySeq = {
    for {
      associations ← byStoreAdmin(admin).map(_.sharedSearchId)
      searches     ← scope.flatMap(SharedSearch.Scope.read) match {
        case Some(s) ⇒ SharedSearches.filter(_.id === associations).filter(_.scope === s)
        case _       ⇒ SharedSearches.filter(_.id === associations)
      }
    } yield searches
  }

  def bySharedSearch(search: SharedSearch): QuerySeq = filter(_.sharedSearchId === search.id)

  def associatedAdmins(search: SharedSearch)(implicit ec: ExecutionContext): StoreAdmins.QuerySeq = {
    for {
      associations ← bySharedSearch(search).map(_.storeAdminId)
      admins       ← StoreAdmins.filter(_.id === associations)
    } yield admins
  }
}
