package services

import java.time.Instant

import failures.NotFoundFailure404
import failures.SharedSearchFailures._
import failures.Util.diffToFailures
import models.sharedsearch._
import models.{StoreAdmin, StoreAdmins}
import payloads.SharedSearchPayloads._
import responses.{StoreAdminResponse, TheResponse}
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.db.DbResultT._
import utils.aliases._

object SharedSearchService {
  def getAll(admin: StoreAdmin, rawScope: Option[String])(implicit ec: EC, db: DB): Result[Seq[SharedSearch]] = (for {
    scope ← * <~ rawScope.toXor(SharedSearchScopeNotFound.single)
    searchScope ← * <~ SharedSearch.Scope.read(scope).toXor(NotFoundFailure404(SharedSearch, scope).single)
    result ← * <~ SharedSearchAssociations.associatedWith(admin, searchScope).result.toXor
  } yield result).run()

  def get(code: String)(implicit ec: EC, db: DB): Result[SharedSearch] =
    mustFindActiveByCode(code).run()

  def getAssociates(code: String)(implicit ec: EC, db: DB): Result[Seq[StoreAdminResponse.Root]] = (for {
    search      ← * <~ mustFindActiveByCode(code)
    associates  ← * <~ SharedSearchAssociations.associatedAdmins(search).result.toXor
  } yield associates.map(StoreAdminResponse.build)).run()

  def create(admin: StoreAdmin, payload: SharedSearchPayload)(implicit ec: EC, db: DB): Result[SharedSearch] = (for {
    search ← * <~ SharedSearches.create(SharedSearch.byAdmin(admin, payload))
    _      ← * <~ SharedSearchAssociations.create(SharedSearchAssociation(sharedSearchId = search.id,
      storeAdminId = admin.id))
  } yield search).runTxn()

  def update(admin: StoreAdmin, code: String, payload: SharedSearchPayload)
    (implicit ec: EC, db: DB): Result[SharedSearch] = (for {
    search  ← * <~ mustFindActiveByCode(code)
    updated ← * <~ SharedSearches.update(search, search.copy(title = payload.title, query = payload.query))
  } yield updated).runTxn()

  def delete(admin: StoreAdmin, code: String)(implicit ec: EC, db: DB): Result[Unit] = (for {
    search ← * <~ mustFindActiveByCode(code)
    _      ← * <~ SharedSearches.update(search, search.copy(deletedAt = Some(Instant.now)))
  } yield ()).runTxn()

  def associate(admin: StoreAdmin, code: String, requestedAssigneeIds: Seq[Int])
    (implicit ec: EC, db: DB, ac: AC): Result[TheResponse[SharedSearch]] = (for {

    search          ← * <~ mustFindActiveByCode(code)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    associates      ← * <~ SharedSearchAssociations.associatedAdmins(search).result.toXor
    newAssociations = adminIds.diff(associates.map(_.id))
      .map(adminId ⇒ SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = adminId))
    _               ← * <~ SharedSearchAssociations.createAll(newAssociations)
    notFoundAdmins  = diffToFailures(requestedAssigneeIds, adminIds, StoreAdmin)
    assignedAdmins  = associates.filter(a ⇒ newAssociations.map(_.storeAdminId).contains(a.id))
    _               ← * <~ LogActivity.associatedWithSearch(admin, search, assignedAdmins)
  } yield TheResponse.build(search, errors = notFoundAdmins)).runTxn()

  def unassociate(admin: StoreAdmin, code: String, assigneeId: Int)
    (implicit ec: EC, db: DB, ac: AC): Result[SharedSearch] = (for {

    search     ← * <~ mustFindActiveByCode(code)
    associate  ← * <~ StoreAdmins.mustFindById404(assigneeId)
    assignment ← * <~ SharedSearchAssociations.byStoreAdmin(associate).one.mustFindOr(SharedSearchAssociationNotFound(code, assigneeId))
    _          ← * <~ SharedSearchAssociations.byStoreAdmin(associate).delete
    _          ← * <~ LogActivity.unassociatedFromSearch(admin, search, associate)
  } yield search).runTxn()

  private def mustFindActiveByCode(code: String)(implicit ec: EC): DbResult[SharedSearch] =
    SharedSearches.findActiveByCode(code).mustFindOr(NotFoundFailure404(SharedSearch, code))
}
