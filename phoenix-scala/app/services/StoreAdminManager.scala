package services

import cats.implicits._
import failures.NotFoundFailure404
import failures.UserFailures._
import models.account._
import models.admin.{AdminData, AdminsData}
import payloads.StoreAdminPayloads._
import responses.StoreAdminResponse
import services.account._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object StoreAdminManager {

  def getById(accountId: Int)(implicit ec: EC, db: DB): DbResultT[StoreAdminResponse.Root] =
    for {
      admin     ← * <~ Users.mustFindByAccountId(accountId)
      adminData ← * <~ AdminsData.mustFindByAccountId(accountId)
    } yield StoreAdminResponse.build(admin, adminData)

  def create(payload: CreateStoreAdminPayload, author: Option[User])(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[StoreAdminResponse.Root] = {

    for {
      organization ← * <~ Organizations
        .findByName(payload.org)
        .mustFindOr(OrganizationNotFoundByName(payload.org))

      context = AccountCreateContext(payload.roles, payload.org, organization.scopeId)

      admin ← * <~ AccountManager.createUser(name = payload.name.some,
                                             email = payload.email.some,
                                             password = payload.password,
                                             context = context)
      organizationScope ← * <~ Scopes.mustFindById400(organization.scopeId)
      scope             ← * <~ Scope.overwrite(organizationScope.path, payload.scope)
      adminUser ← * <~ AdminsData.create(
        AdminData(accountId = admin.accountId,
                  userId = admin.id,
                  state = AdminData.Invited,
                  scope = scope))

      _ ← * <~ LogActivity.storeAdminCreated(admin, author)
    } yield StoreAdminResponse.build(admin, adminUser)
  }

  def update(accountId: Int,
             payload: UpdateStoreAdminPayload,
             author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      admin ← * <~ Users.mustFindByAccountId(accountId)
      _     ← * <~ Users.updateEmailMustBeUnique(payload.email.some, accountId)
      saved ← * <~ Users.update(admin,
                                admin.copy(name = Some(payload.name),
                                           phoneNumber = payload.phoneNumber,
                                           email = Some(payload.email)))
      adminData ← * <~ AdminsData.mustFindByAccountId(accountId)
      _         ← * <~ LogActivity.storeAdminUpdated(saved, author)
    } yield StoreAdminResponse.build(saved, adminData)

  def delete(accountId: Int, author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      adminUser ← * <~ AdminsData.mustFindByAccountId(accountId)
      _ ← * <~ AdminsData
        .deleteById(adminUser.id, DbResultT.unit, i ⇒ NotFoundFailure404(AdminData, i))
      admin  ← * <~ Users.mustFindByAccountId(accountId)
      result ← * <~ Users.deleteById(admin.id, DbResultT.unit, i ⇒ NotFoundFailure404(User, i))
      _      ← * <~ AccountAccessMethods.findByAccountId(accountId).delete
      _      ← * <~ AccountRoles.findByAccountId(accountId).delete
      _      ← * <~ AccountOrganizations.findByAccountId(accountId).delete
      _      ← * <~ Accounts.deleteById(accountId, DbResultT.unit, i ⇒ NotFoundFailure404(Account, i))
      _      ← * <~ LogActivity.storeAdminDeleted(admin, author)
    } yield result

  def changeState(id: Int, payload: StateChangeStoreAdminPayload, author: User)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[StoreAdminResponse.Root] =
    for {
      admin     ← * <~ Users.mustFindByAccountId(id)
      adminUser ← * <~ AdminsData.mustFindByAccountId(id)
      _         ← * <~ adminUser.transitionState(payload.state)
      result    ← * <~ AdminsData.update(adminUser, adminUser.copy(state = payload.state))
      _         ← * <~ LogActivity.storeAdminStateChanged(admin, adminUser.state, result.state, author)
    } yield StoreAdminResponse.build(admin, result)
}
