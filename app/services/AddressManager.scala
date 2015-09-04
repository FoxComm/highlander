package services

import scala.concurrent.ExecutionContext

import cats.data.Validated.{Invalid, Valid}
import models.{Address, Addresses, Region, Regions}
import payloads.CreateAddressPayload
import responses.Addresses.Root
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._

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
      case Invalid(errors) ⇒ Result.failure(errors.head)
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
      case Invalid(errors) ⇒ Result.failure(errors.head)
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
}
