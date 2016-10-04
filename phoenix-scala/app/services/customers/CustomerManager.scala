package services.customers

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._

import failures.CustomerFailures._
import failures.NotFoundFailure404
import models.account._
import models.account.{User, Users}
import models.cord.{OrderShippingAddresses, Orders}
import models.customer.{CustomerUser, CustomerUsers}
import models.customer.CustomerUsers.scope._
import models.customer._

import models.location.Addresses
import models.shipping.Shipments
import payloads.CustomerPayloads._
import responses.CustomerResponse._
import services._
import services.account._
import services.account._
import failures.CustomerFailures._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CustomerManager {

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
      customerUsers ← * <~ CustomerUsers
                       .filter(_.accountId === accountId)
                       .withRegionsAndRank
                       .mustFindOneOr(NotFoundFailure404(CustomerUser, accountId))
      (customerUser, shipRegion, billRegion, rank) = customerUsers
      maxOrdersDate ← * <~ Orders.filter(_.accountId === accountId).map(_.placedAt).max.result
      phoneOverride ← * <~ doOrGood(customer.phoneNumber.isEmpty,
                                    resolvePhoneNumber(accountId),
                                    None)
    } yield
      build(customer.copy(phoneNumber = customer.phoneNumber.orElse(phoneOverride)),
            customerUser,
            shipRegion,
            billRegion,
            rank = rank,
            lastOrderDays = maxOrdersDate.map(DAYS.between(_, Instant.now)))
  }

  def create(payload: CreateCustomerPayload,
             admin: Option[User] = None,
             context: AccountCreateContext)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      user ← * <~ AccountManager.createUser(name = payload.name,
                                            email = payload.email.some,
                                            password = payload.password,
                                            context = context,
                                            checkEmail = !payload.isGuest.getOrElse(false))

      custUser ← * <~ CustomerUsers.create(
                    CustomerUser(accountId = user.accountId,
                                 userId = user.id,
                                 isGuest = payload.isGuest.getOrElse(false)))
      response = build(user, custUser)
      _ ← * <~ LogActivity.customerCreated(response, admin)
    } yield response

  def createGuest(context: AccountCreateContext)(implicit ec: EC,
                                                 db: DB): DbResultT[(User, CustomerUser)] =
    for {

      user ← * <~ AccountManager.createUser(name = None,
                                            email = None,
                                            password = None,
                                            context = context,
                                            checkEmail = false)

      custUser ← * <~ CustomerUsers.create(
                    CustomerUser(accountId = user.accountId, userId = user.id, isGuest = true))
      response = build(user, custUser)
    } yield (user, custUser)

  def update(accountId: Int, payload: UpdateCustomerPayload, admin: Option[User] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate
      customer ← * <~ Users.mustFindByAccountId(accountId)
      _        ← * <~ Users.updateEmailMustBeUnique(payload.email, accountId)
      updated  ← * <~ Users.update(customer, updatedUser(customer, payload))
      custUser ← * <~ CustomerUsers.mustFindByAccountId(accountId)
      _        ← * <~ CustomerUsers.update(custUser, updatedCustUser(custUser, payload))
      _        ← * <~ LogActivity.customerUpdated(customer, updated, admin)
    } yield build(updated, custUser)

  def updatedUser(customer: User, payload: UpdateCustomerPayload): User = {
    customer.copy(name = payload.name.fold(customer.name)(Some(_)),
                  email = payload.email.orElse(customer.email),
                  phoneNumber = payload.phoneNumber.fold(customer.phoneNumber)(Some(_)))
  }

  def updatedCustUser(custUser: CustomerUser, payload: UpdateCustomerPayload): CustomerUser = {
    (payload.name, payload.email) match {
      case (Some(name), Some(email)) ⇒ custUser.copy(isGuest = false)
      case _                         ⇒ custUser
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
      custUser ← * <~ CustomerUsers.mustFindByAccountId(accountId)
      _        ← * <~ CustomerUsers.update(custUser, custUser.copy(isGuest = false))
      response = build(updated, custUser)
      _ ← * <~ LogActivity.customerActivated(response, admin)
    } yield response

  def toggleDisabled(accountId: Int, disabled: Boolean, actor: User)(implicit ec: EC,
                                                                     db: DB,
                                                                     ac: AC): DbResultT[Root] =
    for {
      r        ← * <~ AccountManager.toggleDisabled(accountId, disabled, actor)
      customer ← * <~ Users.mustFindByAccountId(accountId)
      custUser ← * <~ CustomerUsers.mustFindByAccountId(accountId)
    } yield build(customer, custUser)

  def toggleBlacklisted(accountId: Int,
                        blacklisted: Boolean,
                        actor: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      r        ← * <~ AccountManager.toggleBlacklisted(accountId, blacklisted, actor)
      customer ← * <~ Users.mustFindByAccountId(accountId)
      custUser ← * <~ CustomerUsers.mustFindByAccountId(accountId)
    } yield build(customer, custUser)

}
