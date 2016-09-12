package services

import failures.NotFoundFailure404
import failures.StoreAdminFailures.AlreadyExistsWithEmail
import models.account._
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import utils.aliases._
import utils.db._

object StoreAdminManager {

  def getById(accountId: Int)(implicit ec: EC, db: DB): DbResultT[StoreAdminResponse.Root] =
    for {
      admin ← * <~ Users.mustFindByAccountId(id)
    } yield StoreAdminResponse.build(admin)

  def create(payload: CreateStoreAdminPayload,
             author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      _ ← * <~ Users
           .findByEmail(payload.email)
           .mustNotFindOr(AlreadyExistsWithEmail(payload.email))

      account ← * <~ Accounts.create(Account())

      _ ← * <~ (payload.password match {
               case Some(password) ⇒
                 AccountAccessMethods.create(AccountAccessMethod.build("login", password))
               case None ⇒ DbResultT[Unit]
             })

      admin = User(accountId = account.id,
                   email = payload.email,
                   name = payload.name,
                   phoneNumber = payload.phoneNumber)

      newAdmin ← * <~ Users.create(admin)

      adminUser ← * <~ StoreAdminUsers.create(
                     StoreAdminUser(accountId = account.id,
                                    userId = newAdmin.id,
                                    state = StoreAdminUser.Invited))

      _ ← * <~ LogActivity.storeAdminCreated(newAdmin, author)
    } yield StoreAdminResponse.build(saved)

  def update(id: Int,
             payload: UpdateStoreAdminPayload,
             author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      admin ← * <~ Users.mustFindByAccountId(id)
      saved ← * <~ Users.update(admin,
                                admin.copy(name = payload.name,
                                           phoneNumber = payload.phoneNumber,
                                           email = payload.email))
      _ ← * <~ LogActivity.storeAdminUpdated(saved, author)
    } yield StoreAdminResponse.build(saved)

  def delete(accountId: Int, author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      admin  ← * <~ User.mustFindByAccountId(id)
      result ← * <~ Users.deleteById(accountId, DbResultT.unit, i ⇒ NotFoundFailure404(User, i))
      _      ← * <~ LogActivity.storeAdminDeleted(admin, author)
    } yield result

  def changeState(id: Int, payload: StateChangeStoreAdminPayload, author: User)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      admin     ← * <~ StoreAdminUsers.mustFindByAccountId(id)
      adminUser ← * <~ StoreAdminUsers.mustFindByAccountId(id)
      _         ← * <~ adminUser.transitionState(payload.state)
      result    ← * <~ StoreAdminUsers.update(admin, admin.copy(state = payload.state))
      _         ← * <~ LogActivity.storeAdminStateChanged(admin, adminUser.state, result.state, author)
    } yield StoreAdminResponse.build(result)
}
