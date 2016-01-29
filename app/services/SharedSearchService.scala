package services

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.activity.ActivityContext
import models.{StoreAdmins, SharedSearch, SharedSearches, SharedSearchAssociation, SharedSearchAssociations, StoreAdmin}
import payloads.SharedSearchPayload
import responses.{StoreAdminResponse, TheResponse}
import slick.driver.PostgresDriver.api._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick._
import utils.Slick.implicits._

object SharedSearchService {
  def getAll(admin: StoreAdmin, rawScope: Option[String])
    (implicit ec: ExecutionContext, db: Database): Result[Seq[SharedSearch]] = (for {
    result ← * <~ SharedSearchAssociations.associatedWith(admin, rawScope).result.toXor
  } yield result).run()

  def get(code: String)(implicit ec: ExecutionContext, db: Database): Result[SharedSearch] =
    mustFindActiveByCode(code).run()

  def getAssociates(code: String)(implicit ec: ExecutionContext, db: Database): Result[Seq[StoreAdminResponse.Root]] = (for {
    search      ← * <~ mustFindActiveByCode(code)
    associates  ← * <~ SharedSearchAssociations.associatedAdmins(search).result.toXor
  } yield associates.map(StoreAdminResponse.build)).run()

  def create(admin: StoreAdmin, payload: SharedSearchPayload)
    (implicit ec: ExecutionContext, db: Database): Result[SharedSearch] = (for {
    search ← * <~ SharedSearches.create(SharedSearch.byAdmin(admin, payload))
    _      ← * <~ SharedSearchAssociations.create(SharedSearchAssociation(sharedSearchId = search.id,
      storeAdminId = admin.id))
  } yield search).runTxn()

  def update(admin: StoreAdmin, code: String, payload: SharedSearchPayload)
    (implicit ec: ExecutionContext, db: Database): Result[SharedSearch] = (for {
    search  ← * <~ mustFindActiveByCode(code)
    updated ← * <~ SharedSearches.update(search, search.copy(title = payload.title, query = payload.query))
  } yield updated).runTxn()

  def delete(admin: StoreAdmin, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = (for {
    search ← * <~ mustFindActiveByCode(code)
    _      ← * <~ SharedSearches.update(search, search.copy(deletedAt = Some(Instant.now)))
  } yield ()).runTxn()

  def associate(admin: StoreAdmin, code: String, requestedAssigneeIds: Seq[Int])
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[TheResponse[SharedSearch]] = (for {

    search          ← * <~ mustFindActiveByCode(code)
    adminIds        ← * <~ StoreAdmins.filter(_.id.inSetBind(requestedAssigneeIds)).map(_.id).result
    associates      ← * <~ SharedSearchAssociations.associatedAdmins(search).result.toXor
    newAssociations = adminIds.diff(associates.map(_.id))
      .map(adminId ⇒ SharedSearchAssociation(sharedSearchId = search.id, storeAdminId = adminId))
    _               ← * <~ SharedSearchAssociations.createAll(newAssociations)
    warnings        = Failures(requestedAssigneeIds.diff(adminIds).map(NotFoundFailure404(StoreAdmin, _)): _*)
    assignedAdmins  = associates.filter(a ⇒ newAssociations.map(_.storeAdminId).contains(a.id))
    _               ← * <~ LogActivity.associatedWithSearch(admin, search, assignedAdmins)
  } yield TheResponse.build(search, warnings = warnings)).runTxn()

  def unassociate(admin: StoreAdmin, code: String, assigneeId: Int)
    (implicit db: Database, ec: ExecutionContext, ac: ActivityContext): Result[SharedSearch] = (for {

    search     ← * <~ mustFindActiveByCode(code)
    associate  ← * <~ StoreAdmins.mustFindById(assigneeId)
    assignment ← * <~ SharedSearchAssociations.byStoreAdmin(associate).one.mustFindOr(SharedSearchAssociationNotFound(code, assigneeId))
    _          ← * <~ SharedSearchAssociations.byStoreAdmin(associate).delete
    _          ← * <~ LogActivity.unassociatedFromSearch(admin, search, associate)
  } yield search).runTxn()

  private def mustFindActiveByCode(code: String)(implicit ec: ExecutionContext): DbResult[SharedSearch] =
    SharedSearches.findActiveByCode(code).mustFindOr(NotFoundFailure404(SharedSearch, code))
}
