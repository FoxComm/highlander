package services

import java.time.Instant

import cats.implicits._
import failures.NotFoundFailure404
import models.customer._
import models.location.{Address, Addresses}
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import payloads.AddressPayloads._
import responses.AddressResponse
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object AddressManager {

  def findAllByCustomer(originator: Originator, customerId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[AddressResponse]] = {
    val query = originator match {
      case AdminOriginator(_)    ⇒ Addresses.findAllActiveByCustomerIdWithRegions(customerId)
      case CustomerOriginator(_) ⇒ Addresses.findAllByCustomerIdWithRegions(customerId)
    }

    for (records ← * <~ query.result) yield AddressResponse.buildMulti(records)
  }

  def get(originator: Originator, addressId: Int, customerId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[AddressResponse] =
    for {
      address  ← * <~ findByOriginator(originator, addressId, customerId)
      response ← * <~ AddressResponse.fromAddress(address)
    } yield response

  def create(originator: Originator, payload: CreateAddressPayload, customerId: Int)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[AddressResponse] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      address  ← * <~ Addresses.create(Address.fromPayload(payload, customerId))
      response ← * <~ AddressResponse.fromAddress(address)
      _        ← * <~ LogActivity.addressCreated(originator, customer, response)
    } yield response

  def edit(originator: Originator, addressId: Int, customerId: Int, payload: CreateAddressPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[AddressResponse] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      oldAddress ← * <~ Addresses
                    .findActiveByIdAndCustomer(addressId, customerId)
                    .mustFindOneOr(addressNotFound(addressId))
      address     ← * <~ Address.fromPayload(payload, customerId).copy(id = addressId).validate
      _           ← * <~ Addresses.insertOrUpdate(address)
      response    ← * <~ AddressResponse.fromAddress(address)
      oldResponse ← * <~ AddressResponse.fromAddress(oldAddress)
      _           ← * <~ LogActivity.addressUpdated(originator, customer, response, oldResponse)
    } yield response

  def remove(originator: Originator, addressId: Int, customerId: Int)(implicit ec: EC,
                                                                      db: DB,
                                                                      ac: AC): DbResultT[Unit] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      address ← * <~ Addresses
                 .findActiveByIdAndCustomer(addressId, customerId)
                 .mustFindOneOr(addressNotFound(addressId))
      softDelete ← * <~ address.copy(deletedAt = Instant.now.some, isDefaultShipping = false)
      updated    ← * <~ Addresses.update(address, softDelete)
      response   ← * <~ AddressResponse.fromAddress(updated)
      _          ← * <~ LogActivity.addressDeleted(originator, customer, response)
    } yield {}

  def setDefaultShippingAddress(addressId: Int, customerId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[AddressResponse] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      _ ← * <~ Addresses
           .findShippingDefaultByCustomerId(customerId)
           .map(_.isDefaultShipping)
           .update(false)
      address ← * <~ Addresses
                 .findActiveByIdAndCustomer(addressId, customerId)
                 .mustFindOneOr(addressNotFound(addressId))
      newAddress = address.copy(isDefaultShipping = true)
      _        ← * <~ Addresses.update(address, newAddress)
      response ← * <~ AddressResponse.fromAddress(newAddress)
    } yield response

  def removeDefaultShippingAddress(customerId: Int)(implicit ec: EC, db: DB): DbResultT[Int] =
    ExceptionWrapper.wrapDbio(
        Addresses
          .findShippingDefaultByCustomerId(customerId)
          .map(_.isDefaultShipping)
          .update(false))

  private def findByOriginator(originator: Originator, addressId: Int, customerId: Int)(
      implicit ec: EC) = originator match {
    case AdminOriginator(_) ⇒
      Addresses
        .findByIdAndCustomer(addressId, customerId)
        .mustFindOneOr(addressNotFound(addressId))
    case CustomerOriginator(_) ⇒
      Addresses
        .findActiveByIdAndCustomer(addressId, customerId)
        .mustFindOneOr(addressNotFound(addressId))
  }

  private def addressNotFound(id: Int) = NotFoundFailure404(Address, id)
}
