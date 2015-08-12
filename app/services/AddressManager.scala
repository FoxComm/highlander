package services

import scala.concurrent.{ExecutionContext, Future}

import cats.Functor
import models.{Order, Address, Addresses, State, States}
import org.scalactic.{Bad, Good, Or}
import payloads.CreateAddressPayload
import responses.Addresses.Root
import responses.{Addresses ⇒ Response}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Validation.Result.{Failure ⇒ Invalid, Success}

object Temp0 {
  type ServiceResult0[A] = Future[A Or Failures]

  object ServiceResult0 {
    def good[A](order: A): ServiceResult0[A] =
      Future.successful(Good(order).asInstanceOf[A Or Failures])

    def failures(failures: Failure*): ServiceResult0[Nothing] =
      Future.successful(Bad(Failures(failures: _*)))

    def failures(theFailures: Failures): ServiceResult0[Nothing] =
      failures(theFailures: _*)

    def failure(failure: Failure): ServiceResult0[Nothing] =
      failures(failure)
  }
}

import Temp0._

object AddressManager {
  def create(payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): ServiceResult0[Root] = {
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
      case f: Invalid ⇒ ServiceResult0.failure(ValidationFailure(f))
    }
  }

  def edit(addressId: Int, customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database): ServiceResult0[Root] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId, id = addressId)
    address.validate match {
      case Success ⇒
        db.run((for {
          rowsAffected ← Addresses.insertOrUpdate(address)
          state ← States.findById(address.stateId)
        } yield (rowsAffected, address, state)).transactionally).map {
          case (1, address, Some(state)) ⇒ Good(Response.build(address, state))
          case (_, address, Some(state)) ⇒ Bad(NotFoundFailure(address).single)
          case (_, _, None)              ⇒ Bad(NotFoundFailure(State, address.stateId).single)
        }
      case f: Invalid ⇒ ServiceResult0.failure(ValidationFailure(f))
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
