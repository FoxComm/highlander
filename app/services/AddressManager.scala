package services

import java.time.Instant

import cats.implicits._
import models.order._
import Order._
import failures.NotFoundFailure404
import models.customer._
import models.location._
import models.traits.{AdminOriginator, CustomerOriginator, Originator}
import payloads.CreateAddressPayload
import responses.Addresses._
import responses.{TheResponse, Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import utils.aliases._

object AddressManager {

  def findAllByCustomer(originator: Originator, customerId: Int)
    (implicit ec: EC, db: DB, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = {

    val query = originator match {
      case AdminOriginator(_)     ⇒ Addresses.findAllActiveByCustomerIdWithRegions(customerId)
      case CustomerOriginator(_)  ⇒ Addresses.findAllByCustomerIdWithRegions(customerId)
    }

    Addresses.sortedAndPagedWithRegions(query).result.map(Response.buildMulti).toTheResponse.run()
  }

  def get(originator: Originator, addressId: Int, customerId: Int)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    address ← * <~ findByOriginator(originator, addressId, customerId)
    region  ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
  } yield Response.build(address, region)).run()

  def create(originator: Originator, payload: CreateAddressPayload, customerId: Int)
    (implicit ec: EC, db: DB, ac: AC): Result[Root] = (for {

    customer  ← * <~ Customers.mustFindById404(customerId)
    address   ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = customerId))
    region    ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _         ← * <~ LogActivity.addressCreated(originator, customer, address, region)
  } yield Response.build(address, region)).runTxn()

  def edit(originator: Originator, addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: EC, db: DB, ac: AC): Result[Root] = (for {

    customer    ← * <~ Customers.mustFindById404(customerId)
    oldAddress  ← * <~ Addresses.findActiveByIdAndCustomer(addressId, customerId).one.mustFindOr(addressNotFound(addressId))
    oldRegion   ← * <~ Regions.findOneById(oldAddress.regionId).safeGet.toXor
    address     ← * <~ Address.fromPayload(payload).copy(customerId = customerId, id = addressId).validate
    _           ← * <~ Addresses.insertOrUpdate(address).toXor
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _           ← * <~ LogActivity.addressUpdated(originator, customer, address, region, oldAddress, oldRegion)
  } yield Response.build(address, region)).runTxn()

  def remove(originator: Originator, addressId: Int, customerId: Int)
    (implicit ec: EC, db: DB, ac: AC): Result[Unit] = (for {

    customer    ← * <~ Customers.mustFindById404(customerId)
    address     ← * <~ Addresses.findActiveByIdAndCustomer(addressId, customerId).one.mustFindOr(addressNotFound(addressId))
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    softDelete  ← * <~ address.updateTo(address.copy(deletedAt = Instant.now.some, isDefaultShipping = false))
    updated     ← * <~ Addresses.update(address, softDelete)
    _           ← * <~ LogActivity.addressDeleted(originator, customer, address, region)
  } yield {}).runTxn()

  def setDefaultShippingAddress(addressId: Int, customerId: Int)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    customer    ← * <~ Customers.mustFindById404(customerId)
    _           ← * <~ Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
    address     ← * <~ Addresses.findActiveByIdAndCustomer(addressId, customerId).one.mustFindOr(addressNotFound
    (addressId))
    newAddress  = address.copy(isDefaultShipping = true)
    address     ← * <~ Addresses.update(address, newAddress)
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
  } yield Response.build(newAddress, region)).run()

  def removeDefaultShippingAddress(customerId: Int)
    (implicit ec: EC, db: DB): Result[Int] =
    Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false).run()
      .flatMap(Result.good)

  private def findByOriginator(originator: Originator, addressId: Int, customerId: Int)
    (implicit ec: EC, db: DB) = originator match {
    case AdminOriginator(_) ⇒
      Addresses.findByIdAndCustomer(addressId, customerId).one.mustFindOr(addressNotFound(addressId))
    case CustomerOriginator(_) ⇒
      Addresses.findActiveByIdAndCustomer(addressId, customerId).one.mustFindOr(addressNotFound(addressId))
  }

  private def addressNotFound(id: Int) = NotFoundFailure404(Address, id)
}
