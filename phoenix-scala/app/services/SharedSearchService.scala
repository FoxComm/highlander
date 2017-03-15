package services

import java.time.Instant

import failures.NotFoundFailure404
import failures.SharedSearchFailures._
import failures.Util.diffToFailures
import models.sharedsearch._
import models.account._
import payloads.SharedSearchPayloads._
import responses.{UserResponse, TheResponse}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object SharedSearchService {
  def getAll(admin: User, rawScope: Option[String])(implicit ec: EC,
                                                    db: DB): DbResultT[Seq[SharedSearch]] =
    for {
      scope ← * <~ rawScope.toXor(SharedSearchScopeNotFound.single)
      searchScope ← * <~ SharedSearch.Scope
                     .read(scope)
                     .toXor(NotFoundFailure404(SharedSearch, scope).single)
      result ← * <~ SharedSearchAssociations.associatedWith(admin, searchScope).result
    } yield result

  def get(code: String)(implicit ec: EC, db: DB): DbResultT[SharedSearch] =
    mustFindActiveByCode(code)

  def getAssociates(code: String)(implicit ec: EC, db: DB): DbResultT[Seq[UserResponse.Root]] =
    for {
      search     ← * <~ mustFindActiveByCode(code)
      associates ← * <~ SharedSearchAssociations.associatedAdmins(search).result
    } yield associates.map(UserResponse.build)

  def create(admin: User, payload: SharedSearchPayload)(implicit ec: EC,
                                                        db: DB,
                                                        au: AU): DbResultT[SharedSearch] =
    for {
      search ← * <~ SharedSearches.create(SharedSearch.byAdmin(admin, payload, Scope.current))
      _ ← * <~ SharedSearchAssociations.create(
             SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = admin.accountId))
    } yield search

  def update(admin: User, code: String, payload: SharedSearchPayload)(
      implicit ec: EC,
      db: DB): DbResultT[SharedSearch] =
    for {
      search ← * <~ mustFindActiveByCode(code)
      updated ← * <~ SharedSearches
                 .update(search, search.copy(title = payload.title, query = payload.query))
    } yield updated

  def delete(admin: User, code: String)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      search ← * <~ mustFindActiveByCode(code)
      _      ← * <~ SharedSearches.update(search, search.copy(deletedAt = Some(Instant.now)))
    } yield ()

  def associate(admin: User, code: String, requestedAssigneeIds: Seq[Int])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[TheResponse[SharedSearch]] =
    for {
      search ← * <~ mustFindActiveByCode(code)
      adminIds ← * <~ Users
                  .filter(_.accountId.inSetBind(requestedAssigneeIds))
                  .map(_.accountId)
                  .result
      associates ← * <~ SharedSearchAssociations.associatedAdmins(search).result
      newAssociations = adminIds
        .diff(associates.map(_.id))
        .map(adminId ⇒ SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = adminId))
      _ ← * <~ SharedSearchAssociations.createAll(newAssociations)
      notFoundAdmins = diffToFailures(requestedAssigneeIds, adminIds, User)
      assignedAdmins = associates.filter(a ⇒ newAssociations.map(_.storeAdminId).contains(a.id))
      _ ← * <~ LogActivity().associatedWithSearch(admin, search, assignedAdmins)
    } yield TheResponse.build(search, errors = notFoundAdmins)

  def unassociate(admin: User, code: String, assigneeId: Int)(implicit ec: EC,
                                                              db: DB,
                                                              ac: AC): DbResultT[SharedSearch] =
    for {
      search    ← * <~ mustFindActiveByCode(code)
      associate ← * <~ Users.mustFindByAccountId(assigneeId)
      assignment ← * <~ SharedSearchAssociations
                    .byStoreAdmin(associate)
                    .mustFindOneOr(SharedSearchAssociationNotFound(code, assigneeId))
      _ ← * <~ SharedSearchAssociations.byStoreAdmin(associate).delete
      _ ← * <~ LogActivity().unassociatedFromSearch(admin, search, associate)
    } yield search

  private def mustFindActiveByCode(code: String)(implicit ec: EC): DbResultT[SharedSearch] =
    SharedSearches.findActiveByCode(code).mustFindOr(NotFoundFailure404(SharedSearch, code))
}
