package phoenix.services

import cats.implicits._
import core.db._
import core.failures.NotFoundFailure404
import phoenix.failures.UserFailures._
import phoenix.models.account._
import phoenix.models.admin.{AdminData, AdminsData}
import phoenix.models.customer._
import phoenix.payloads.StoreAdminPayloads._
import phoenix.responses.users.StoreAdminResponse
import phoenix.services.account._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object StoreAdminManager {

  def getById(accountId: Int)(implicit ec: EC, db: DB): DbResultT[StoreAdminResponse] =
    for {
      admin        ← * <~ Users.mustFindByAccountId(accountId)
      adminData    ← * <~ AdminsData.mustFindByAccountId(accountId)
      organization ← * <~ Organizations.mustFindByAccountId(accountId)
    } yield StoreAdminResponse.build(admin, adminData, organization)

  def create(payload: CreateStoreAdminPayload,
             author: Option[User])(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse] =
    for {
      organization ← * <~ Organizations
                      .findByName(payload.org)
                      .mustFindOr(OrganizationNotFoundByName(payload.org))
      roles   = if (payload.roles.isEmpty) List(Authenticator.ADMIN_ROLE) else payload.roles
      context = AccountCreateContext(roles, payload.org, organization.scopeId)

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
      _ ← * <~ CustomersData.create(
           CustomerData(accountId = admin.accountId, userId = admin.id, isGuest = false, scope = scope))
      pwSet ← * <~ doOrGood(payload.password.isEmpty,
                            AccountManager.sendResetPassword(admin, payload.email).map(Option(_)),
                            None)

      _ ← * <~ LogActivity().storeAdminCreated(admin, author, pwSet.map(_.code))
    } yield StoreAdminResponse.build(admin, adminUser, organization)

  def update(accountId: Int,
             payload: UpdateStoreAdminPayload,
             author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse] =
    for {
      admin ← * <~ Users.mustFindByAccountId(accountId)
      _     ← * <~ Users.updateEmailMustBeUnique(payload.email.some, accountId)
      saved ← * <~ Users.update(admin,
                                admin.copy(name = Some(payload.name),
                                           phoneNumber = payload.phoneNumber,
                                           email = Some(payload.email)))
      adminData    ← * <~ AdminsData.mustFindByAccountId(accountId)
      organization ← * <~ Organizations.mustFindByAccountId(accountId)
      _            ← * <~ LogActivity().storeAdminUpdated(saved, author)
    } yield StoreAdminResponse.build(saved, adminData, organization)

  def delete(accountId: Int, author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      _ ← * <~ AdminsData
           .filter(_.accountId === accountId)
           .deleteAll(().pure[DbResultT], DbResultT.failure[Unit](UserWithAccountNotFound(accountId)))
      _ ← * <~ CustomersData
           .filter(_.accountId === accountId)
           .deleteAll(().pure[DbResultT], DbResultT.failure[Unit](UserWithAccountNotFound(accountId)))
      admin  ← * <~ Users.mustFindByAccountId(accountId)
      _      ← * <~ UserPasswordResets.filter(_.accountId === accountId).delete
      result ← * <~ Users.deleteById(admin.id, ().pure[DbResultT], i ⇒ NotFoundFailure404(User, i))
      _      ← * <~ AccountAccessMethods.findByAccountId(accountId).delete
      _      ← * <~ AccountRoles.findByAccountId(accountId).delete
      _      ← * <~ AccountOrganizations.filterByAccountId(accountId).delete
      _      ← * <~ Accounts.deleteById(accountId, ().pure[DbResultT], i ⇒ NotFoundFailure404(Account, i))
      _      ← * <~ LogActivity().storeAdminDeleted(admin, author)
    } yield result

  def changeState(id: Int,
                  payload: StateChangeStoreAdminPayload,
                  author: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[StoreAdminResponse] =
    for {
      admin        ← * <~ Users.mustFindByAccountId(id)
      adminUser    ← * <~ AdminsData.mustFindByAccountId(id)
      organization ← * <~ Organizations.mustFindByAccountId(id)
      _            ← * <~ adminUser.transitionState(payload.state)
      result       ← * <~ AdminsData.update(adminUser, adminUser.copy(state = payload.state))
      _            ← * <~ LogActivity().storeAdminStateChanged(admin, adminUser.state, result.state, author)
    } yield StoreAdminResponse.build(admin, result, organization)
}
