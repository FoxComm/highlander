package services

import failures.NotFoundFailure404
import failures.StoreAdminFailures.AlreadyExistsWithEmail
import models.traits.Originator
import models.{StoreAdmin, StoreAdmins}
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import utils.aliases._
import utils.db._

object StoreAdminManager {

  def getById(id: Int)(implicit ec: EC, db: DB): DbResultT[StoreAdminResponse.Root] =
    for {
      admin ← * <~ StoreAdmins.mustFindById404(id)
    } yield StoreAdminResponse.build(admin)

  def create(
      payload: CreateStoreAdminPayload,
      author: Originator)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      _ ← * <~ StoreAdmins
           .findByEmail(payload.email)
           .mustNotFindOr(AlreadyExistsWithEmail(payload.email))
      admin = StoreAdmin.build(email = payload.email,
                               password = payload.password,
                               name = payload.name,
                               department = payload.department)
      saved ← * <~ StoreAdmins.create(admin)
      _     ← * <~ LogActivity.storeAdminCreated(saved, author)
    } yield StoreAdminResponse.build(saved)

  def update(id: Int, payload: UpdateStoreAdminPayload, author: Originator)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      admin ← * <~ StoreAdmins.mustFindById404(id)
      saved ← * <~ StoreAdmins.update(admin,
                                      admin.copy(name = payload.name,
                                                 department = payload.department,
                                                 email = payload.email))
      _ ← * <~ LogActivity.storeAdminUpdated(saved, author)
    } yield StoreAdminResponse.build(saved)

  def delete(id: Int, author: Originator)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      admin ← * <~ StoreAdmins.mustFindById404(id)
      result ← * <~ StoreAdmins.deleteById(id, DbResultT.unit, i ⇒
                    NotFoundFailure404(StoreAdmin, i))
      _ ← * <~ LogActivity.storeAdminDeleted(admin, author)
    } yield result
}
