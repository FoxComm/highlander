package services

import failures.NotFoundFailure404
import failures.StoreAdminFailures.AlreadyExistsWithEmail
import models.account._
import models.admin.{StoreAdminUsers, StoreAdminUser}
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import utils.aliases._
import utils.db._

object StoreAdminManager {

  def getById(accountId: Int)(implicit ec: EC, db: DB): DbResultT[StoreAdminResponse.Root] =
    for {
      admin          ← * <~ Users.mustFindByAccountId(accountId)
      storeAdminUser ← * <~ StoreAdminUsers.mustFindByAccountId(accountId)
    } yield StoreAdminResponse.build(admin, storeAdminUser)

  def create(payload: CreateStoreAdminPayload,
             author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      _ ← * <~ Users
           .findByEmail(payload.email)
           .one
           .mustNotFindOr(AlreadyExistsWithEmail(payload.email))

      account ← * <~ Accounts.create(Account())

      _ ← * <~ (payload.password match {
               case Some(password) ⇒
                 AccountAccessMethods.create(AccountAccessMethod
                       .build(accountId = account.id, name = "login", password = password))
               case None ⇒ DbResultT.unit
             })

      admin = User(accountId = account.id,
                   email = Some(payload.email),
                   name = Some(payload.name),
                   phoneNumber = payload.phoneNumber)

      newAdmin ← * <~ Users.create(admin)

      adminUser ← * <~ StoreAdminUsers.create(
                     StoreAdminUser(accountId = account.id,
                                    userId = newAdmin.id,
                                    state = StoreAdminUser.Invited))

      _ ← * <~ LogActivity.storeAdminCreated(newAdmin, author)
    } yield StoreAdminResponse.build(newAdmin, adminUser)

  def update(accountId: Int,
             payload: UpdateStoreAdminPayload,
             author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      admin ← * <~ Users.mustFindByAccountId(accountId)
      saved ← * <~ Users.update(admin,
                                admin.copy(name = Some(payload.name),
                                           phoneNumber = payload.phoneNumber,
                                           email = Some(payload.email)))
      storeAdminUser ← * <~ StoreAdminUsers.mustFindByAccountId(accountId)
      _              ← * <~ LogActivity.storeAdminUpdated(saved, author)
    } yield StoreAdminResponse.build(saved, storeAdminUser)

  def delete(accountId: Int, author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      admin  ← * <~ Users.mustFindByAccountId(accountId)
      result ← * <~ Users.deleteById(accountId, DbResultT.unit, i ⇒ NotFoundFailure404(User, i))
      _      ← * <~ LogActivity.storeAdminDeleted(admin, author)
    } yield result

  def changeState(id: Int, payload: StateChangeStoreAdminPayload, author: User)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      admin     ← * <~ Users.mustFindByAccountId(id)
      adminUser ← * <~ StoreAdminUsers.mustFindByAccountId(id)
      _         ← * <~ adminUser.transitionState(payload.state)
      result    ← * <~ StoreAdminUsers.update(adminUser, adminUser.copy(state = payload.state))
      _         ← * <~ LogActivity.storeAdminStateChanged(admin, adminUser.state, result.state, author)
    } yield StoreAdminResponse.build(admin, result)
}
