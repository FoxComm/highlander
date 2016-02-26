package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._
import models.order._
import Order._
import models.customer._
import models.location._
import models.traits.Originator
import payloads.CreateAddressPayload
import responses.Addresses._
import responses.{Addresses ⇒ Response, TheResponse}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import models.activity.ActivityContext

object AddressManager {

  def findAllByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[TheResponse[Seq[Root]]] = {
    
    val query = Addresses.findAllActiveByCustomerIdWithRegions(customerId)
    Addresses.sortedAndPagedWithRegions(query).result.map(Response.buildMulti).toTheResponse.run()
  }

  def create(originator: Originator, payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {

    customer  ← * <~ Customers.mustFindById404(customerId)
    address   ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = customerId))
    region    ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _         ← * <~ LogActivity.addressCreated(originator, customer, address, region)
  } yield Response.build(address, region)).runTxn()

  def edit(originator: Originator, addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {

    customer    ← * <~ Customers.mustFindById404(customerId)
    oldAddress  ← * <~ Addresses.findActiveByIdAndCustomer(addressId, customerId)
                                .mustFindOr(NotFoundFailure404(Address, addressId))
    oldRegion   ← * <~ Regions.findOneById(oldAddress.regionId).safeGet.toXor
    address     ← * <~ Address.fromPayload(payload).copy(customerId = customerId, id = addressId).validate
    _           ← * <~ Addresses.insertOrUpdate(address).toXor
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _           ← * <~ LogActivity.addressUpdated(originator, customer, address, region, oldAddress, oldRegion)
  } yield Response.build(address, region)).runTxn()

  def get(customerId: Int, addressId: Int)(implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    address ← * <~ Addresses.findActiveByIdAndCustomer(addressId, customerId)
                            .mustFindOr(NotFoundFailure404(Address, addressId))
    region  ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
  } yield Response.build(address, region)).run()

  def remove(originator: Originator, customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Unit] = (for {

    customer    ← * <~ Customers.mustFindById404(customerId)
    address     ← * <~ Addresses.findActiveByIdAndCustomer(addressId, customerId)
                                .mustFindOr(NotFoundFailure404(Address, addressId))
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    softDelete  ← * <~ address.updateTo(address.copy(deletedAt = Instant.now.some, isDefaultShipping = false))
    updated     ← * <~ Addresses.update(address, softDelete)
    _           ← * <~ LogActivity.addressDeleted(originator, customer, address, region)
  } yield {}).runTxn()

  def setDefaultShippingAddress(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {
    customer    ← * <~ Customers.mustFindById404(customerId)
    _           ← * <~ Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
    address     ← * <~ Addresses.findActiveByIdAndCustomer(addressId, customerId)
                                .mustFindOr(NotFoundFailure404(Address, addressId))
    newAddress  = address.copy(isDefaultShipping = true)
    address     ← * <~ Addresses.update(address, newAddress)
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
  } yield Response.build(newAddress, region)).run()

  def removeDefaultShippingAddress(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Int] =
    Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false).run()
      .flatMap(Result.good)

  def getDisplayAddress(customer: Customer)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Root]] = {

    defaultShipping(customer.id).run().flatMap {
      case Some((address, region)) ⇒
        Future.successful(Response.build(address, region).some)
      case None ⇒
        lastShippedTo(customer.id).run().map {
          case Some((ship, region)) ⇒ Response.buildOneShipping(ship, region, isDefault = false).some
          case None ⇒ None
        }
    }
  }

  def defaultShipping(customerId: Int): DBIO[Option[(Address, Region)]] = (for {
    address ← Addresses.findShippingDefaultByCustomerId(customerId)
    region  ← Regions if region.id === address.regionId
  } yield (address, region)).one

  def lastShippedTo(customerId: Int)
    (implicit db: Database, ec: ExecutionContext): DBIO[Option[(OrderShippingAddress, Region)]] = (for {
    order ← Orders.findByCustomerId(customerId)
      .filter(_.state =!= (Order.Cart: Order.State))
      .sortBy(_.id.desc)
    shipping ← OrderShippingAddresses if shipping.orderId === order.id
    region   ← Regions if region.id === shipping.regionId
  } yield (shipping, region)).take(1).one
}
