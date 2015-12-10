package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.implicits._
import models.Order._
import models.{StoreAdmin, Address, Addresses, Customer, Customers, Order, OrderShippingAddress,
OrderShippingAddresses, Orders, Region, Regions}
import payloads.CreateAddressPayload
import responses.Addresses._
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._

import models.activity.ActivityContext

object AddressManager {

  private def addressNotFound(id: Int): NotFoundFailure404 = NotFoundFailure404(Address, id)

  def findAllVisibleByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    val query = Addresses.findAllVisibleByCustomerIdWithRegions(customerId)

    Addresses.sortedAndPagedWithRegions(query).result.map(Response.build)
  }

  def create(payload: CreateAddressPayload, customerId: Int, admin: Option[StoreAdmin] = None)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {

    customer  ← * <~ Customers.mustFindById(customerId)
    address   ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = customerId))
    region    ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _         ← * <~ LogActivity.addressCreated(admin, customer, address, region)
  } yield Response.build(address, region)).runT()

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Root] = (for {

    customer    ← * <~ Customers.mustFindById(customerId)
    oldAddress  ← * <~ Addresses.findByIdAndCustomer(addressId, customerId).mustFindOr(addressNotFound(addressId))
    oldRegion   ← * <~ Regions.findOneById(oldAddress.regionId).safeGet.toXor
    address     ← * <~ Address.fromPayload(payload).copy(customerId = customerId, id = addressId).validate
    _           ← * <~ Addresses.insertOrUpdate(address).toXor
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    _           ← * <~ LogActivity.addressUpdated(admin, customer, address, region, oldAddress, oldRegion)
  } yield Response.build(address, region)).runT()

  def get(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    address ← * <~ Addresses.findByIdAndCustomer(addressId, customerId).mustFindOr(addressNotFound(addressId))
    region  ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
  } yield Response.build(address, region, address.isDefaultShipping.some)).runT()

  def remove(customerId: Int, addressId: Int, admin: StoreAdmin)
    (implicit ec: ExecutionContext, db: Database, ac: ActivityContext): Result[Unit] = (for {

    customer    ← * <~ Customers.mustFindById(customerId)
    address     ← * <~ Addresses.findByIdAndCustomer(addressId, customerId).mustFindOr(addressNotFound(addressId))
    region      ← * <~ Regions.findOneById(address.regionId).safeGet.toXor
    softDelete  ← * <~ address.updateTo(address.copy(deletedAt = Instant.now.some, isDefaultShipping = false))
    updated     ← * <~ Addresses.update(address, softDelete)
    _           ← * <~ LogActivity.addressDeleted(admin, customer, address, region)
  } yield {}).runT()

  def setDefaultShippingAddress(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = (for {
    _           ← Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
    newDefault  ← Addresses.findById(addressId).extract.map(_.isDefaultShipping).update(true)
  } yield newDefault).transactionally.run().flatMap {
    case rowsAffected if rowsAffected == 1 ⇒ Result.unit
    case _ ⇒ Result.failure(NotFoundFailure404(Address, addressId))
  }

  def removeDefaultShippingAddress(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Int] =
    Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false).run()
      .flatMap(Result.good)

  def getDisplayAddress(customer: Customer)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Root]] = {

    defaultShipping(customer.id).run().flatMap {
      case Some((address, region)) ⇒
        Future.successful(Response.build(address, region, true.some).some)
      case None ⇒
        lastShippedTo(customer.id).run().map {
          case Some((ship, region)) ⇒ Response.buildOneShipping(ship, region, false).some
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
      .filter(_.status =!= (Order.Cart: models.Order.Status))
      .sortBy(_.id.desc)
    shipping ← OrderShippingAddresses if shipping.orderId === order.id
    region   ← Regions if region.id === shipping.regionId
  } yield (shipping, region)).take(1).one
}
