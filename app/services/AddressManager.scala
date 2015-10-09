package services

import java.time.Instant

import scala.concurrent.{Future, ExecutionContext}

import cats.data.Validated.{Invalid, Valid}
import cats.implicits._
import models.{OrderShippingAddress, Order, Orders, Customer, OrderShippingAddresses, Address, Addresses, Region,
Regions}
import models.Order._
import payloads.CreateAddressPayload
import responses.Addresses.Root
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import cats.implicits._

import utils.time.JavaTimeSlickMapper.instantAndTimestampWithoutZone

object AddressManager {
  def create(payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId)
    address.validate match {
      case Valid(_) ⇒
        db.run(for {
          newAddress ← Addresses.save(address)
          region     ← Regions.findById(newAddress.regionId)
        } yield (newAddress, region)).flatMap {
          case (address, Some(region))  ⇒ Result.good(Response.build(address, region))
          case (_, None)                ⇒ Result.failure(NotFoundFailure(Region, address.regionId))
        }
      case Invalid(errors) ⇒ Result.failures(errors)
    }
  }

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId, id = addressId)
    address.validate match {
      case Valid(_) ⇒
        db.run((for {
          rowsAffected ← Addresses.insertOrUpdate(address)
          region       ← Regions.findById(address.regionId)
        } yield (rowsAffected, address, region)).transactionally).flatMap {
          case (1, address, Some(region)) ⇒ Result.good(Response.build(address, region))
          case (_, address, Some(region)) ⇒ Result.failure(NotFoundFailure(address))
          case (_, _, None)               ⇒ Result.failure(NotFoundFailure(Region, address.regionId))
        }
      case Invalid(errors) ⇒ Result.failures(errors)
    }
  }

  def get(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
      val query = ( for {
        Some(address) ← Addresses.findById(customerId, addressId).one
        region  ← Regions.findById(address.regionId)
      } yield (address, region))

      db.run(query).flatMap {
          case (address, Some(region)) ⇒ Result.good(Response.build(address, region, Some(address.isDefaultShipping)))
          case (address, None)         ⇒ Result.failure(NotFoundFailure(Region, address.regionId))
          case (_, _)                  ⇒ Result.failure(NotFoundFailure(Address,addressId))
      }
  }


  def remove(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    val query =
      Addresses.findById(customerId, addressId)
      .map{ a ⇒ (a.deletedAt, a.isDefaultShipping)}
      .update((Some(Instant.now()), false))  //set delete time and set default to false

      db.run(query).flatMap{
        case 1 ⇒ Result.unit
        case _ ⇒ Result.failure(NotFoundFailure(Address, addressId))
      }
  }

  def setDefaultShippingAddress(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {
    db.run((for {
      _ ← Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
      newDefault ← Addresses._findById(addressId).extract.map(_.isDefaultShipping).update(true)
    } yield newDefault).transactionally).flatMap {
      case rowsAffected if rowsAffected == 1 ⇒ Result.unit
      case _                                 ⇒ Result.failure(NotFoundFailure(Address, addressId))
    }
  }

  def removeDefaultShippingAddress(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Int] =
    db.run(Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)).flatMap(Result
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
