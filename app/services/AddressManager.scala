package services

import java.time.Instant

import scala.concurrent.{Future, ExecutionContext}

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import cats.implicits._
import models.{OrderShippingAddress, Order, Orders, Customer, OrderShippingAddresses, Address, Addresses, Region,
Regions}
import models.Order._
import payloads.CreateAddressPayload
import responses.Addresses.Root
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives.SortAndPage
import utils.Slick.DbResult
import utils.Slick.implicits._
import cats.implicits._

import utils.DbResultT.*
import utils.DbResultT.implicits._

import utils.time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

object AddressManager {

  private def addressNotFound(id: Int): NotFoundFailure404 = NotFoundFailure404(Address, id)

  def findAllVisibleByCustomer(customerId: Int)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): ResultWithMetadata[Seq[Root]] = {
    val query = Addresses.findAllVisibleByCustomerIdWithRegions(customerId)

    Addresses.sortedAndPagedWithRegions(query).result.map(Response.build)
  }

  def create(payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    address ← * <~ Addresses.create(Address.fromPayload(payload).copy(customerId = customerId))
    region  ← * <~ Regions.mustFindById(address.regionId)
  } yield Response.build(address, region)).value.run()

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    address ← * <~ Address.fromPayload(payload).copy(customerId = customerId, id = addressId).validate
    _       ← * <~ Addresses.insertOrUpdate(address).toXor
    region  ← * <~ Regions.mustFindById(address.regionId)
  } yield Response.build(address, region)).value.run()

  def get(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = (for {

    address ← * <~ Addresses.findByIdAndCustomer(addressId, customerId).mustFindOr { addressNotFound(addressId) }
    region  ← * <~ Regions.mustFindById(address.regionId)
  }  yield Response.build(address, region, address.isDefaultShipping.some)).value.run()

  def remove(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = (for {

    address     ← * <~ Addresses.findByIdAndCustomer(addressId, customerId).mustFindOr { addressNotFound(addressId) }
    softDelete  ← * <~ address.updateTo(address.copy(deletedAt = Instant.now.some, isDefaultShipping = false))
    updated     ← * <~ Addresses.update(address, softDelete)
  } yield {}).value.run()

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
    (Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)).run().flatMap(Result
      .good)

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
