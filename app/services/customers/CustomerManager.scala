package services.customers

import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

import cats.implicits._
import failures.NotFoundFailure404
import models.StoreAdmin
import models.customer.Customers.scope._
import models.customer.{Customer, Customers}
import models.location.Addresses
import models.order.{OrderShippingAddresses, Orders}
import models.shipping.Shipments
import payloads.CustomerPayloads._
import responses.CustomerResponse.{Root, build}
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

  private def resolvePhoneNumber(customerId: Int)(implicit ec: EC): DbResultT[Option[String]] = {
    def resolveFromShipments(customerId: Int) =
      (for {
        order    ← Orders if order.customerId === customerId
        shipment ← Shipments if shipment.orderRef === order.referenceNumber &&
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
      shipment ← * <~ (if (default.isEmpty) resolveFromShipments(customerId)
                       else DbResultT.good(default))
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
      phoneOverride ← * <~ (if (customer.phoneNumber.isEmpty) resolvePhoneNumber(id)
                            else DbResultT.good(None))
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
      _ ← * <~ (if (!payload.isGuest.getOrElse(false))
                  Customers.createEmailMustBeUnique(customer.email)
                else DbResultT.unit)
      updated ← * <~ Customers.create(customer)
      response = build(updated)
      _ ← * <~ LogActivity.customerCreated(response, admin)
    } yield response

  def update(customerId: Int, payload: UpdateCustomerPayload, admin: Option[StoreAdmin] = None)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      _        ← * <~ payload.validate.toXor
      customer ← * <~ Customers.mustFindById404(customerId)
      _ ← * <~ payload.email
           .map(Customers.updateEmailMustBeUnique(_, customerId))
           .getOrElse(DbResultT.unit)
      updated ← * <~ Customers.update(
                   customer,
                   customer.copy(name = payload.name.fold(customer.name)(Some(_)),
                                 email = payload.email.getOrElse(customer.email),
                                 phoneNumber =
                                   payload.phoneNumber.fold(customer.phoneNumber)(Some(_))))
      _ ← * <~ LogActivity.customerUpdated(customer, updated, admin)
    } yield build(updated)

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
