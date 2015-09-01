package services

import scala.concurrent.{ExecutionContext, Future}

import cats.Functor
import cats.data.Validated.{Valid, Invalid}
import cats.data.Xor
import models.{Order, Address, Addresses, Region, Regions}
import payloads.CreateAddressPayload
import responses.Addresses.Root
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
// import utils.Validation.Result.{Failure ⇒ Invalid, Success}

object AddressManager {
  def create(payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId)
    address.validateNew match {
      case Valid(_) ⇒
        db.run(for {
          newAddress ← Addresses.save(address)
          region     ← Regions.findById(newAddress.regionId)
        } yield (newAddress, region)).map {
          case (address, Some(region))  ⇒ Xor.right(Response.build(address, region))
          case (_, None)                ⇒ Xor.left(NotFoundFailure(Region, address.regionId).single)
        }
      case Invalid(errors) ⇒ Result.failure(ValidationFailureNew(errors))
    }
  }

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database): Result[Root] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId, id = addressId)
    address.validateNew match {
      case Valid(_) ⇒
        db.run((for {
          rowsAffected ← Addresses.insertOrUpdate(address)
          region       ← Regions.findById(address.regionId)
        } yield (rowsAffected, address, region)).transactionally).map {
          case (1, address, Some(region)) ⇒ Xor.right(Response.build(address, region))
          case (_, address, Some(region)) ⇒ Xor.left(NotFoundFailure(address).single)
          case (_, _, None)               ⇒ Xor.left(NotFoundFailure(Region, address.regionId).single)
        }
      case Invalid(errors) ⇒ Result.failure(ValidationFailureNew(errors))
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
