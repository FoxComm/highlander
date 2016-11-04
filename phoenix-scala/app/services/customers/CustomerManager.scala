package services.customers

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import org.json4s.native.Serialization._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.directives.CookieDirectives.setCookie
import akka.http.scaladsl.server.directives.RespondWithDirectives.respondWithHeader
import cats.implicits._
import failures.AuthFailures.ChangePasswordFailed
import failures.CustomerFailures._
import failures.{NotFoundFailure400, NotFoundFailure404}
import models.account._
import models.cord.{OrderShippingAddresses, Orders}
import models.customer.CustomersData.scope._
import models.customer._
import models.location.Addresses
import models.shipping.Shipments
import payloads.CustomerPayloads._
import responses.CustomerResponse._
import services._
import failures.UserFailures.AccessMethodNotFound
import models.auth.UserToken
import services.account._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.JsonFormatters._

object CustomerManager {

  implicit val formatters = phoenixFormats

  private def resolvePhoneNumber(accountId: Int)(implicit ec: EC): DbResultT[Option[String]] = {
    def resolveFromShipments(accountId: Int) =
      (for {
        order    ← Orders if order.accountId === accountId
        shipment ← Shipments if shipment.cordRef === order.referenceNumber &&
          shipment.shippingAddressId.isDefined
        address ← OrderShippingAddresses if address.id === shipment.shippingAddressId &&
          address.phoneNumber.isDefined
      } yield (address.phoneNumber, shipment.updatedAt)).sortBy {
        case (_, updatedAt)   ⇒ updatedAt.desc.nullsLast
      }.map { case (phone, _) ⇒ phone }.one.map(_.flatten).dbresult

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

  def getByAccountId(accountId: Int)(implicit ec: EC, db: DB): DbResultT[Root] = {
    for {
      customer ← * <~ Users.mustFindByAccountId(accountId)
      customerDatas ← * <~ CustomersData
                       .filter(_.accountId === accountId)
                       .withRegionsAndRank
                       .mustFindOneOr(NotFoundFailure404(CustomerData, accountId))
      (customerData, shipRegion, billRegion, rank) = customerDatas
      maxOrdersDate ← * <~ Orders.filter(_.accountId === accountId).map(_.placedAt).max.result
      phoneOverride ← * <~ doOrGood(customer.phoneNumber.isEmpty,
                                    resolvePhoneNumber(accountId),
                                    None)
    } yield
      build(customer.copy(phoneNumber = customer.phoneNumber.orElse(phoneOverride)),
            customerData,
            shipRegion,
            billRegion,
            rank = rank,
            lastOrderDays = maxOrdersDate.map(DAYS.between(_, Instant.now)))
  }

  def create(payload: CreateCustomerPayload,
             admin: Option[User] = None,
             context: AccountCreateContext)(implicit ec: EC,
                                            db: DB,
                                            ac: AC): DbResultT[(Root, AuthPayload)] =
    for {
      user ← * <~ AccountManager.createUser(name = payload.name,
                                            email = payload.email.toLowerCase.some,
                                            password = payload.password,
                                            context = context,
                                            checkEmail = !payload.isGuest.getOrElse(false))

      custData ← * <~ CustomersData.create(
                    CustomerData(accountId = user.accountId,
                                 userId = user.id,
                                 isGuest = payload.isGuest.getOrElse(false)))
      result = build(user, custData)
      _        ← * <~ LogActivity.customerCreated(result, admin)
      account  ← * <~ Accounts.mustFindById400(user.accountId)
      claimSet ← * <~ AccountManager.getClaims(account.id, context.scopeId)
      token    ← * <~ UserToken.fromUserAccount(user, account, claimSet)
      auth     ← * <~ AuthPayload(token)
    } yield (result, auth)

  def createGuest(context: AccountCreateContext)(implicit ec: EC,
                                                 db: DB): DbResultT[(User, CustomerData)] =
    for {

      user ← * <~ AccountManager.createUser(name = None,
                                            email = None,
                                            password = None,
                                            context = context,
                                            checkEmail = false)

      custData ← * <~ CustomersData.create(
                    CustomerData(accountId = user.accountId, userId = user.id, isGuest = true))
      response = build(user, custData)
    } yield (user, custData)

  def update(accountId: Int, payload: UpdateCustomerPayload, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[(Root, AuthPayload)] =
    for {
      _        ← * <~ payload.validate
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _        ← * <~ Users.updateEmailMustBeUnique(payload.email.map(_.toLowerCase), accountId)
      updated  ← * <~ Users.update(customer, updatedUser(customer, payload))
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
      _        ← * <~ CustomersData.update(custData, updatedCustUser(custData, payload))
      _        ← * <~ LogActivity.customerUpdated(customer, updated, admin)
      account  ← * <~ Accounts.mustFindById400(updated.accountId)
      ao ← * <~ AccountOrganizations
            .findByAccountId(account.id)
            .mustFindOneOr(NotFoundFailure400(AccountOrganizations, account.id))
      org      ← * <~ Organizations.mustFindById400(ao.organizationId)
      claimSet ← * <~ AccountManager.getClaims(account.id, org.id)
      token    ← * <~ UserToken.fromUserAccount(updated, account, claimSet)
      auth     ← * <~ AuthPayload(token)
    } yield (build(updated, custData), auth)

  def changePassword(
      accountId: Int,
      payload: ChangeCustomerPasswordPayload)(implicit ec: EC, db: DB, ac: AC): DbResultT[Unit] =
    for {
      _       ← * <~ payload.validate
      user    ← * <~ Users.mustFindByAccountId(accountId)
      account ← * <~ Accounts.mustFindById404(accountId)
      accessMethod ← * <~ AccountAccessMethods
                      .findOneByAccountIdAndName(account.id, "login")
                      .mustFindOr(AccessMethodNotFound("login"))

      _ ← * <~ failIf(!accessMethod.checkPassword(payload.oldPassword), ChangePasswordFailed)

      updatedAccess ← * <~ AccountAccessMethods
                       .update(accessMethod, accessMethod.updatePassword(payload.newPassword))
      _ ← * <~ LogActivity.userPasswordReset(user)
    } yield {}

  def updatedUser(customer: User, payload: UpdateCustomerPayload): User = {
    customer.copy(name = payload.name.fold(customer.name)(Some(_)),
                  email = payload.email.map(_.toLowerCase).orElse(customer.email),
                  phoneNumber = payload.phoneNumber.fold(customer.phoneNumber)(Some(_)))
  }

  def updatedCustUser(custData: CustomerData, payload: UpdateCustomerPayload): CustomerData = {
    (payload.name, payload.email) match {
      case (Some(name), Some(email)) ⇒ custData.copy(isGuest = false)
      case _                         ⇒ custData
    }
  }

  def activate(accountId: Int,
               payload: ActivateCustomerPayload,
               admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _ ← * <~ (customer.email match {
               case None ⇒ DbResultT.failure(CustomerMustHaveCredentials)
               case _    ⇒ DbResultT.unit
             })
      _        ← * <~ Users.updateEmailMustBeUnique(customer.email, accountId)
      updated  ← * <~ Users.update(customer, customer.copy(name = payload.name.some))
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
      _        ← * <~ CustomersData.update(custData, custData.copy(isGuest = false))
      response = build(updated, custData)
      _ ← * <~ LogActivity.customerActivated(response, admin)
    } yield response

  def toggleDisabled(accountId: Int, disabled: Boolean, actor: User)(implicit ec: EC,
                                                                     db: DB,
                                                                     ac: AC): DbResultT[Root] =
    for {
      r        ← * <~ AccountManager.toggleDisabled(accountId, disabled, actor)
      customer ← * <~ Users.mustFindByAccountId(accountId)
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
    } yield build(customer, custData)

  def toggleBlacklisted(accountId: Int,
                        blacklisted: Boolean,
                        actor: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      r        ← * <~ AccountManager.toggleBlacklisted(accountId, blacklisted, actor)
      customer ← * <~ Users.mustFindByAccountId(accountId)
      custData ← * <~ CustomersData.mustFindByAccountId(accountId)
    } yield build(customer, custData)

}
