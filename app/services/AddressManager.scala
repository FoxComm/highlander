package services

import java.time.Instant

import cats.implicits._
import failures.NotFoundFailure404
import models.customer._
import models.location._
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import payloads.AddressPayloads._
import responses.Addresses._
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object AddressManager {

  def findAllByCustomer(originator: Originator, customerId: Int)(implicit ec: EC,
                                                                 db: DB): DbResultT[Seq[Root]] = {
    val query = originator match {
      case AdminOriginator(_)    ⇒ Addresses.findAllActiveByCustomerIdWithRegions(customerId)
      case CustomerOriginator(_) ⇒ Addresses.findAllByCustomerIdWithRegions(customerId)
    }

    for (records ← * <~ query.result.toXor) yield Response.buildMulti(records)
  }

  def get(originator: Originator, addressId: Int, customerId: Int)(implicit ec: EC,
                                                                   db: DB): DbResultT[Root] =
    for {
      address ← * <~ findByOriginator(originator, addressId, customerId)
      region  ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    } yield Response.build(address, region)

  def create(originator: Originator, payload: CreateAddressPayload, customerId: Int)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      address  ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = customerId))
      region   ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
      _        ← * <~ LogActivity.addressCreated(originator, customer, address, region)
    } yield Response.build(address, region)

  def edit(originator: Originator, addressId: Int, customerId: Int, payload: CreateAddressPayload)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[Root] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      oldAddress ← * <~ Addresses
                    .findActiveByIdAndCustomer(addressId, customerId)
                    .mustFindOneOr(addressNotFound(addressId))
      oldRegion ← * <~ Regions.findOneById(oldAddress.regionId).safeGet.toXor
      address ← * <~ Address
                 .fromPayload(payload)
                 .copy(customerId = customerId, id = addressId)
                 .validate
      _      ← * <~ Addresses.insertOrUpdate(address).toXor
      region ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
      _ ← * <~ LogActivity
           .addressUpdated(originator, customer, address, region, oldAddress, oldRegion)
    } yield Response.build(address, region)

  def remove(originator: Originator, addressId: Int, customerId: Int)(implicit ec: EC,
                                                                      db: DB,
                                                                      ac: AC): DbResultT[Unit] =
    for {
      customer ← * <~ Customers.mustFindById404(customerId)
      address ← * <~ Addresses
                 .findActiveByIdAndCustomer(addressId, customerId)
                 .mustFindOneOr(addressNotFound(addressId))
      region ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
      softDelete ← * <~ address.updateTo(
                      address.copy(deletedAt = Instant.now.some, isDefaultShipping = false))
      updated ← * <~ Addresses.update(address, softDelete)
      _       ← * <~ LogActivity.addressDeleted(originator, customer, address, region)
    } yield {}

  def setDefaultShippingAddress(addressId: Int, customerId: Int)(implicit ec: EC,
                                                                 db: DB): DbResultT[Root] =
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
      address ← * <~ Addresses.update(address, newAddress)
      region  ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    } yield Response.build(newAddress, region)

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
