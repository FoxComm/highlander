package services

import scala.concurrent.{ExecutionContext, Future}

import models.{Address, Addresses, State, States}
import org.scalactic.{Bad, Good, Or}
import payloads.CreateAddressPayload
import responses.Addresses.Root
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Validation.Result.{Failure ⇒ Invalid, Success}

object AddressManager {
  def create(payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Root Or Failures] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId)
    address.validate match {
      case Success ⇒
        db.run(for {
          newAddress ← Addresses.save(address)
          state ← States.findById(newAddress.stateId)
        } yield (newAddress, state)).map {
          case (address, Some(state)) ⇒ Good(Response.build(address, state))
          case (_, None)              ⇒ Bad(NotFoundFailure(State, address.stateId).single)
        }
      case f: Invalid ⇒ Future.successful(Bad(ValidationFailure(f).single))
    }
  }

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database): Future[Root Or Failures] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId, id = addressId)
    address.validate match {
      case Success ⇒
        db.run((for {
          rowsAffected ← Addresses.insertOrUpdate(address)
          state ← States.findById(address.stateId)
        } yield (rowsAffected, address, state)).transactionally).map {
          case (n, address, Some(state)) ⇒
            if (n == 1) {
              Good(Response.build(address, state))
            } else {
              Bad(NotFoundFailure(address).single)
            }
          case (_, _, None)              ⇒
            Bad(NotFoundFailure(State, address.stateId).single)
        }
      case f: Invalid ⇒ Future.successful(Bad(ValidationFailure(f).single))
    }
  }

  def setDefaultShippingAddress(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Failure]] = {
    db.run((for {
      _ ← Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
      newDefault ← Addresses._findById(addressId).extract.map(_.isDefaultShipping).update(true)
    } yield newDefault).transactionally).map {
      case rowsAffected if rowsAffected == 1 ⇒
        None
      case _ ⇒
        Some(NotFoundFailure(Address, addressId))
    }
  }

  def removeDefaultShippingAddress(customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Int] =
    db.run(Addresses.findShippingDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false))
}
