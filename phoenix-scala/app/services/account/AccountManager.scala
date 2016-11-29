package services.account

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import failures.NotFoundFailure404
import failures.UserFailures._
import models.cord.{OrderShippingAddresses, Orders}
import models.account._
import models.location.Addresses
import models.shipping.Shipments
import payloads.UserPayloads._
import responses.UserResponse._
import services._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class AccountCreateContext(roles: List[String], org: String, scopeId: Int)

object AccountManager {

  def toggleDisabled(accountId: Int, disabled: Boolean, actor: User)(implicit ec: EC,
                                                                     db: DB,
                                                                     ac: AC): DbResultT[Root] =
    for {
      user ← * <~ Users.mustFindByAccountId(accountId)
      updated ← * <~ Users.update(user,
                                  user.copy(isDisabled = disabled, disabledBy = Some(actor.id)))
      _ ← * <~ LogActivity.userDisabled(disabled, user, actor)
    } yield build(updated)

  // TODO: add blacklistedReason later
  def toggleBlacklisted(accountId: Int,
                        blacklisted: Boolean,
                        actor: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      user ← * <~ Users.mustFindByAccountId(accountId)
      updated ← * <~ Users.update(
                   user,
                   user.copy(isBlacklisted = blacklisted, blacklistedBy = Some(actor.id)))
      _ ← * <~ LogActivity.userBlacklisted(blacklisted, user, actor)
    } yield build(updated)

  def resetPasswordSend(
      email: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[ResetPasswordSendAnswer] =
    for {
      user ← * <~ Users
              .activeUserByEmail(Option(email))
              .mustFindOneOr(NotFoundFailure404(User, email))
      resetPwInstance ← * <~ UserPasswordReset
                         .optionFromUser(user)
                         .toXor(UserHasNoEmail(user.id).single)
      findOrCreate ← * <~ UserPasswordResets
                      .findActiveByEmail(email)
                      .one
                      .findOrCreateExtended(UserPasswordResets.create(resetPwInstance))
      (resetPw, foundOrCreated) = findOrCreate
      updatedResetPw ← * <~ (foundOrCreated match {
                            case Found ⇒
                              UserPasswordResets.update(resetPw, resetPw.updateCode())
                            case Created ⇒ DbResultT.good(resetPw)
                          })
      _ ← * <~ LogActivity.userRemindPassword(user, updatedResetPw.code)
    } yield ResetPasswordSendAnswer(status = "ok")

  def resetPassword(
      code: String,
      newPassword: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[ResetPasswordDoneAnswer] = {
    for {
      remind ← * <~ UserPasswordResets
                .findActiveByCode(code)
                .mustFindOr(ResetPasswordCodeInvalid(code))
      user    ← * <~ Users.mustFindByAccountId(remind.accountId)
      account ← * <~ Accounts.mustFindById404(user.accountId)
      accessMethod ← * <~ AccountAccessMethods
                      .findOneByAccountIdAndName(account.id, "login")
                      .findOrCreate(AccountAccessMethods.create(
                              AccountAccessMethod.buildInitial(account.id)))
      _ ← * <~ Users.update(user, user.copy(isMigrated = false))
      _ ← * <~ UserPasswordResets.update(remind,
                                         remind.copy(state = UserPasswordReset.PasswordRestored,
                                                     activatedAt = Instant.now.some))
      updatedAccess ← * <~ AccountAccessMethods.update(accessMethod,
                                                       accessMethod.updatePassword(newPassword))
      _ ← * <~ LogActivity.userPasswordReset(user)
    } yield ResetPasswordDoneAnswer(status = "ok")
  }

  def getById(accountId: Int)(implicit ec: EC, db: DB): DbResultT[Root] = {
    for {
      user ← * <~ Users.mustFindByAccountId(accountId)
    } yield build(user)
  }

  def createUser(name: Option[String],
                 email: Option[String],
                 password: Option[String],
                 context: AccountCreateContext,
                 checkEmail: Boolean = true,
                 isMigrated: Boolean = false)(implicit ec: EC, db: DB): DbResultT[User] = {

    for {
      scope ← * <~ Scopes.mustFindById404(context.scopeId)
      organization ← * <~ Organizations
                      .findByNameInScope(context.org, scope.id)
                      .mustFindOr(OrganizationNotFound(context.org, scope.path))

      _ ← * <~ (if (checkEmail) (email match {
                  case Some(e) ⇒
                    for {
                      _ ← * <~ Users.createEmailMustBeUnique(e)
                    } yield {}
                  case None ⇒ DbResultT.unit

                })
                else DbResultT.unit)

      account ← * <~ Accounts.create(Account())

      _ ← * <~ (password match {
               case Some(p) ⇒
                 AccountAccessMethods.create(AccountAccessMethod.build(account.id, "login", p))
               case None ⇒ {
                 DbResultT.unit
               }
             })

      user ← * <~ Users.create(
                User(accountId = account.id, email = email, name = name, isMigrated = isMigrated))

      _ ← * <~ AccountOrganizations.create(
             AccountOrganization(accountId = account.id, organizationId = organization.id))

      _ ← * <~ context.roles.map { r ⇒
           addRole(account, r, scope)
         }
    } yield user
  }

  def addRole(account: Account, role: String, scope: Scope)(implicit ec: EC): DbResultT[Unit] = {
    //MAXDO Add claim check here.
    for {
      role ← * <~ Roles
              .findByNameInScope(role, scope.id)
              .mustFindOr(RoleNotFound(role, scope.path))
      _ ← * <~ AccountRoles.create(AccountRole(accountId = account.id, roleId = role.id))
    } yield Unit
  }

  def getClaims(accountId: Int, scopeId: Int)(implicit ec: EC,
                                              db: DB): DbResultT[Account.ClaimSet] =
    for {
      scope        ← * <~ Scopes.mustFindById404(scopeId)
      accountRoles ← * <~ AccountRoles.findByAccountId(accountId).result
      roleIds = accountRoles.map(_.roleId)
      roles ← * <~ Roles.filterByScopeId(roleIds, scopeId).result
      scopedRoleIds = roles.map(_.id)
      roleNames     = roles.map(_.name)
      rolePermissions ← * <~ RolePermissions.findByRoles(scopedRoleIds).result
      permissionIds = rolePermissions.map(_.permissionId)
      permissions ← * <~ Permissions.filter(_.id.inSet(permissionIds)).result
      claims ← * <~ permissions
                .groupBy(_.frn)
                .mapValues(_.map(_.actions))
                .mapValues(_.flatten.toList)
    } yield Account.ClaimSet(scope = scope.path, roles = roleNames, claims = claims)
}
