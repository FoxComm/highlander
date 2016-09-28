package services.customers

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import failures.CustomerFailures._
import failures.NotFoundFailure404
import models.StoreAdmin
import models.cord.{OrderShippingAddresses, Orders}
import models.customer.Customers.scope._
import models.customer._
import models.location.Addresses
import models.shipping.Shipments
import payloads.CustomerPayloads._
import responses.CustomerResponse._
import services._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CustomerManager {

  def toggleDisabled(customerId: Int,
                     disabled: Boolean,
                     admin: StoreAdmin)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      updated ← * <~ Customers.update(
                   customer,
                   customer.copy(isDisabled = disabled, disabledBy = Some(admin.id)))
      _ ← * <~ LogActivity.customerDisabled(disabled, customer, admin)
    } yield build(updated)

  // TODO: add blacklistedReason later
  def toggleBlacklisted(customerId: Int,
                        blacklisted: Boolean,
                        admin: StoreAdmin)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      updated ← * <~ Customers.update(
                   customer,
                   customer.copy(isBlacklisted = blacklisted, blacklistedBy = Some(admin.id)))
      _ ← * <~ LogActivity.customerBlacklisted(blacklisted, customer, admin)
    } yield build(updated)

  def resetPasswordSend(
      email: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[ResetPasswordSendAnswer] =
    for {
      customer ← * <~ Customers
                  .activeCustomerByEmail(Option(email))
                  .mustFindOneOr(NotFoundFailure404(Customer, email))
      resetPwInstance ← * <~ CustomerPasswordReset
                         .optionFromCustomer(customer)
                         .toXor(CustomerHasNoEmail(customer.id).single)
      findOrCreate ← * <~ CustomerPasswordResets
                      .findActiveByEmail(email)
                      .one
                      .findOrCreateExtended(CustomerPasswordResets.create(resetPwInstance))
      (resetPw, foundOrCreated) = findOrCreate
      updatedResetPw ← * <~ (foundOrCreated match {
                            case Found ⇒
                              CustomerPasswordResets.update(resetPw, resetPw.updateCode())
                            case Created ⇒ DbResultT.good(resetPw)
                          })
      _ ← * <~ LogActivity.customerRemindPassword(customer, updatedResetPw.code)
    } yield ResetPasswordSendAnswer(status = "ok")

  def resetPassword(
      code: String,
      newPassword: String)(implicit ec: EC, db: DB, ac: AC): DbResultT[ResetPasswordDoneAnswer] = {
    for {
      remind ← * <~ CustomerPasswordResets
                .findActiveByCode(code)
                .mustFindOr(ResetPasswordCodeInvalid(code))
      customer ← * <~ Customers.mustFindById404(remind.customerId)
      _ ← * <~ CustomerPasswordResets.update(remind,
                                             remind.copy(state =
                                                           CustomerPasswordReset.PasswordRestored,
                                                         activatedAt = Instant.now.some))
      updatedCustomer ← * <~ Customers.update(customer, customer.updatePassword(newPassword))
      _               ← * <~ LogActivity.customerPasswordReset(updatedCustomer)
    } yield ResetPasswordDoneAnswer(status = "ok")
  }

  private def resolvePhoneNumber(customerId: Int)(implicit ec: EC): DbResultT[Option[String]] = {
    def resolveFromShipments(customerId: Int) =
      (for {
        order    ← Orders if order.customerId === customerId
        shipment ← Shipments if shipment.cordRef === order.referenceNumber &&
          shipment.shippingAddressId.isDefined
        address ← OrderShippingAddresses if address.id === shipment.shippingAddressId &&
          address.phoneNumber.isDefined
      } yield (address.phoneNumber, shipment.updatedAt)).sortBy {
        case (_, updatedAt)   ⇒ updatedAt.desc.nullsLast
      }.map { case (phone, _) ⇒ phone }.one.map(_.flatten).toXor

    for {
      default ← * <~ Addresses
                 .filter(address ⇒ address.customerId === customerId && address.isDefaultShipping)
                 .map(_.phoneNumber)
                 .one
                 .map(_.flatten)
                 .toXor
      shipment ← * <~ doOrGood(default.isEmpty, resolveFromShipments(customerId), default)
    } yield shipment
  }

  def getById(id: Int)(implicit ec: EC, db: DB): DbResultT[Root] = {
    for {
      customers ← * <~ Customers
                   .filter(_.id === id)
                   .withRegionsAndRank
                   .mustFindOneOr(NotFoundFailure404(Customer, id))
      (customer, shipRegion, billRegion, rank) = customers
      maxOrdersDate ← * <~ Orders.filter(_.customerId === id).map(_.placedAt).max.result
      phoneOverride ← * <~ doOrGood(customer.phoneNumber.isEmpty, resolvePhoneNumber(id), None)
    } yield
      build(customer.copy(phoneNumber = customer.phoneNumber.orElse(phoneOverride)),
            shipRegion,
            billRegion,
            rank = rank,
            lastOrderDays = maxOrdersDate.map(DAYS.between(_, Instant.now)))
  }

  def create(payload: CreateCustomerPayload,
             admin: Option[StoreAdmin] = None)(implicit ec: EC, db: DB, ac: AC): DbResultT[Root] =
    for {
      customer ← * <~ Customer.buildFromPayload(payload).validate
      _ ← * <~ doOrMeh(!payload.isGuest.getOrElse(false),
                       Customers.createEmailMustBeUnique(customer.email))
      updated ← * <~ Customers.create(customer)
      response = build(updated)
      _ ← * <~ LogActivity.customerCreated(response, admin)
    } yield response

  def update(customerId: Int, payload: UpdateCustomerPayload, admin: Option[StoreAdmin] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate
      customer ← * <~ Customers.mustFindById404(customerId)
      _        ← * <~ Customers.updateEmailMustBeUnique(payload.email, customerId)
      updated  ← * <~ Customers.update(customer, updatedCustomer(customer, payload))
      _        ← * <~ LogActivity.customerUpdated(customer, updated, admin)
    } yield build(updated)

  def updatedCustomer(customer: Customer, payload: UpdateCustomerPayload): Customer = {
    val updatedCustomer = (payload.name, payload.email) match {
      case (Some(name), Some(email)) ⇒ customer.copy(isGuest = false)
      case _                         ⇒ customer
    }

    updatedCustomer.copy(name = payload.name.fold(customer.name)(Some(_)),
                         email = payload.email.orElse(customer.email),
                         phoneNumber = payload.phoneNumber.fold(customer.phoneNumber)(Some(_)))
  }

  def activate(customerId: Int, payload: ActivateCustomerPayload, admin: StoreAdmin)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate
      customer ← * <~ Customers.mustFindById404(customerId)
      _        ← * <~ Customers.updateEmailMustBeUnique(customer.email, customer.id)
      updated ← * <~ Customers.update(customer,
                                      customer.copy(name = payload.name.some, isGuest = false))
      response = build(updated)
      _ ← * <~ LogActivity.customerActivated(response, admin)
    } yield response
}
