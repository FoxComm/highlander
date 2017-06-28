package phoenix.services.customers

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.db._
import core.failures.{NotFoundFailure400, NotFoundFailure404}
import phoenix.failures.AuthFailures.ChangePasswordFailed
import phoenix.failures.CustomerFailures._
import phoenix.failures.UserFailures.AccessMethodNotFound
import phoenix.models.account._
import phoenix.models.auth.UserToken
import phoenix.models.cord.{OrderShippingAddresses, Orders}
import phoenix.models.customer.CustomersData.scope._
import phoenix.models.customer._
import phoenix.models.location.Addresses
import phoenix.models.shipping.Shipments
import phoenix.payloads.AuthPayload
import phoenix.payloads.CustomerPayloads._
import phoenix.responses.GroupResponses.CustomerGroupResponse
import phoenix.responses.users.CustomerResponse
import phoenix.services._
import phoenix.services.account._
import phoenix.utils.JsonFormatters._
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object CustomerManager {

  implicit val formatters = phoenixFormats

  def resolvePhoneNumber(accountId: Int)(implicit ec: EC): DbResultT[Option[String]] = {
    def resolveFromShipments(accountId: Int) =
      (for {
        order    ← Orders if order.accountId === accountId
        shipment ← Shipments if shipment.cordRef === order.referenceNumber &&
          shipment.shippingAddressId.isDefined
        address ← OrderShippingAddresses if address.id === shipment.shippingAddressId &&
          address.phoneNumber.isDefined
      } yield (address.phoneNumber, shipment.updatedAt))
        .sortBy {
          case (_, updatedAt) ⇒ updatedAt.desc.nullsLast
        }
        .map { case (phone, _) ⇒ phone }
        .one
        .map(_.flatten)
        .dbresult

    for {
      default ← * <~ Addresses
                 .filter(address ⇒ address.accountId === accountId && address.isDefaultShipping)
                 .map(_.phoneNumber)
                 .one
                 .map(_.flatten)
                 .dbresult
      shipment ← * <~ doOrGood(default.isEmpty, resolveFromShipments(accountId), default)
    } yield shipment
  }

  private def getCustomerInfo(userDbT: DbResultT[User])(implicit ec: EC,
                                                        db: DB): DbResultT[CustomerResponse] =
    for {
      customer ← * <~ userDbT
      customerDatas ← * <~ CustomersData
                       .filter(_.accountId === customer.accountId)
                       .withRegionsAndRank
                       .mustFindOneOr(NotFoundFailure404(CustomerData, customer.accountId))
      (customerData, shipRegion, billRegion, rank) = customerDatas
      maxOrdersDate ← * <~ Orders
                       .filter(_.accountId === customer.accountId)
                       .map(_.placedAt)
                       .max
                       .result
      totals ← * <~ StoreCreditService.fetchTotalsForCustomer(customer.accountId)
      phoneOverride ← * <~ doOrGood(customer.phoneNumber.isEmpty,
                                    resolvePhoneNumber(customer.accountId),
                                    None)
      groupMembership ← * <~ CustomerGroupMembers.findByCustomerDataId(customerData.id).result
      groupIds = groupMembership.map(_.groupId).toSet
      groups ← * <~ CustomerGroups.findAllByIds(groupIds).result
    } yield
      CustomerResponse.build(
        customer.copy(phoneNumber = customer.phoneNumber.orElse(phoneOverride)),
        customerData,
        shipRegion,
        billRegion,
        rank = rank,
        scTotals = totals,
        lastOrderDays = maxOrdersDate.map(DAYS.between(_, Instant.now)),
        groups = groups.map(CustomerGroupResponse.build)
      )

  def getByAccountId(accountId: Int)(implicit ec: EC, db: DB): DbResultT[CustomerResponse] = {
    val userDbByAccountId = Users.mustFindByAccountId(accountId)
    getCustomerInfo(userDbByAccountId)
  }

  def getByEmail(email: String)(implicit ec: EC, db: DB): DbResultT[CustomerResponse] = {
    val userDbByEmail = Users.mustfindOneByEmail(email)
    getCustomerInfo(userDbByEmail)
  }

  def create(payload: CreateCustomerPayload, admin: Option[User] = None, context: AccountCreateContext)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[(CustomerResponse, AuthPayload)] =
    for {

      contextScope ← * <~ Scopes.mustFindById400(context.scopeId)
      scope        ← * <~ Scope.overwrite(contextScope.path, payload.scope)

      customer ← * <~ createCustomer(payload, admin, context, scope)
      (user, custData) = customer
      result           = CustomerResponse.build(user, custData)
      _        ← * <~ LogActivity().withScope(scope).customerCreated(result, admin)
      account  ← * <~ Accounts.mustFindById400(user.accountId)
      claimSet ← * <~ AccountManager.getClaims(account.id, context.scopeId)
      token    ← * <~ UserToken.fromUserAccount(user, account, claimSet)
      auth     ← * <~ AuthPayload(token)
    } yield (result, auth)

  def createFromAdmin(
      payload: CreateCustomerPayload,
      admin: Option[User] = None,
      context: AccountCreateContext)(implicit ec: EC, db: DB, ac: AC): DbResultT[CustomerResponse] =
    for {
      contextScope ← * <~ Scopes.mustFindById400(context.scopeId)
      scope        ← * <~ Scope.overwrite(contextScope.path, payload.scope)

      result ← * <~ createCustomer(payload, admin, context, scope)
      (user, customerData) = result
      resp                 = CustomerResponse.build(user, customerData)
      _ ← * <~ LogActivity().withScope(scope).customerCreated(resp, admin)
    } yield resp

  private def createCustomer(payload: CreateCustomerPayload,
                             admin: Option[User] = None,
                             context: AccountCreateContext,
                             scope: LTree)(implicit ec: EC, db: DB, ac: AC): DbResultT[(User, CustomerData)] =
    for {
      user ← * <~ AccountManager.createUser(name = payload.name,
                                            email = payload.email.toLowerCase.some,
                                            password = payload.password,
                                            context = context,
                                            checkEmail = !payload.isGuest.getOrElse(false))

      custData ← * <~ CustomersData.create(
                  CustomerData(accountId = user.accountId,
                               userId = user.id,
                               isGuest = payload.isGuest.getOrElse(false),
                               scope = scope))
    } yield (user, custData)

  def createGuest(context: AccountCreateContext)(implicit ec: EC, db: DB): DbResultT[(User, CustomerData)] =
    for {

      user ← * <~ AccountManager.createUser(name = None,
                                            email = None,
                                            password = None,
                                            context = context,
                                            checkEmail = false)
      scope ← * <~ Scopes.mustFindById400(context.scopeId)
      custData ← * <~ CustomersData.create(
                  CustomerData(accountId = user.accountId,
                               userId = user.id,
                               isGuest = true,
                               scope = LTree(scope.path)))
    } yield (user, custData)

  def update(accountId: Int, payload: UpdateCustomerPayload, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[(CustomerResponse, AuthPayload)] =
    for {
      result ← * <~ updateCustomer(accountId, payload, admin)
      (updated, custData) = result
      account ← * <~ Accounts.mustFindById400(updated.accountId)
      ao ← * <~ AccountOrganizations
            .filterByAccountId(account.id)
            .mustFindOneOr(NotFoundFailure400(AccountOrganizations, account.id))
      org      ← * <~ Organizations.mustFindById400(ao.organizationId)
      claimSet ← * <~ AccountManager.getClaims(account.id, org.id)
      token    ← * <~ UserToken.fromUserAccount(updated, account, claimSet)
      auth     ← * <~ AuthPayload(token)
    } yield (CustomerResponse.build(updated, custData), auth)

  def updateFromAdmin(accountId: Int, payload: UpdateCustomerPayload, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[CustomerResponse] =
    for {
      result ← * <~ updateCustomer(accountId, payload, admin)
      (user, customerData) = result
    } yield CustomerResponse.build(user, customerData)

  private def updateCustomer(accountId: Int, payload: UpdateCustomerPayload, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[(User, CustomerData)] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
      _ ← * <~ (if (custData.isGuest) DbResultT.unit
                else Users.updateEmailMustBeUnique(payload.email.map(_.toLowerCase), accountId))
      updated ← * <~ Users.update(customer, updatedUser(customer, payload))
      _       ← * <~ CustomersData.update(custData, updatedCustUser(custData, payload))
      _       ← * <~ LogActivity().customerUpdated(customer, updated, admin)
    } yield (updated, custData)

  def changePassword(accountId: Int, payload: ChangeCustomerPasswordPayload)(implicit ec: EC,
                                                                             db: DB,
                                                                             ac: AC): DbResultT[Unit] =
    for {
      user    ← * <~ Users.mustFindByAccountId(accountId)
      account ← * <~ Accounts.mustFindById404(accountId)
      accessMethod ← * <~ AccountAccessMethods
                      .findOneByAccountIdAndName(account.id, "login")
                      .mustFindOr(AccessMethodNotFound("login"))

      _ ← * <~ failIf(!accessMethod.checkPassword(payload.oldPassword), ChangePasswordFailed)

      updatedAccess ← * <~ AccountAccessMethods
                       .update(accessMethod, accessMethod.updatePassword(payload.newPassword))
      _ ← * <~ LogActivity().userPasswordReset(user)
    } yield {}

  def updatedUser(customer: User, payload: UpdateCustomerPayload): User =
    customer.copy(
      name = payload.name.fold(customer.name)(Some(_)),
      email = payload.email.map(_.toLowerCase).orElse(customer.email),
      phoneNumber = payload.phoneNumber.fold(customer.phoneNumber)(Some(_))
    )

  def updatedCustUser(custData: CustomerData, payload: UpdateCustomerPayload): CustomerData =
    (payload.name, payload.email) match {
      case (Some(_), Some(_)) ⇒ custData.copy(isGuest = false)
      case _                  ⇒ custData
    }

  def activate(accountId: Int,
               payload: ActivateCustomerPayload,
               admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[CustomerResponse] =
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _ ← * <~ (customer.email match {
           case None ⇒ DbResultT.failure(CustomerMustHaveCredentials)
           case _    ⇒ DbResultT.unit
         })
      _        ← * <~ Users.updateEmailMustBeUnique(customer.email, accountId)
      updated  ← * <~ Users.update(customer, customer.copy(name = payload.name.some))
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
      _        ← * <~ CustomersData.update(custData, custData.copy(isGuest = false))
      response = CustomerResponse.build(updated, custData)
      _ ← * <~ LogActivity().customerActivated(response, admin)
    } yield response

  def toggleDisabled(accountId: Int, disabled: Boolean, actor: User)(implicit ec: EC,
                                                                     db: DB,
                                                                     ac: AC): DbResultT[CustomerResponse] =
    for {
      r        ← * <~ AccountManager.toggleDisabled(accountId, disabled, actor)
      customer ← * <~ Users.mustFindByAccountId(accountId)
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
    } yield CustomerResponse.build(customer, custData)

  def toggleBlacklisted(accountId: Int,
                        blacklisted: Boolean,
                        actor: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[CustomerResponse] =
    for {
      r        ← * <~ AccountManager.toggleBlacklisted(accountId, blacklisted, actor)
      customer ← * <~ Users.mustFindByAccountId(accountId)
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
    } yield CustomerResponse.build(customer, custData)

}
