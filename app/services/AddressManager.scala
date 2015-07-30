package services

import scala.concurrent.{ExecutionContext, Future}

import org.postgresql.util.PSQLException
import org.scalactic.{Or, Good, Bad}
import utils.Validation.Result.{Failure ⇒ Invalid, Success}
import models.{Addresses, OrderShippingAddress, CreditCards, OrderShippingAddresses, Address, States, State, Customer}
import payloads.CreateAddressPayload
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import responses.{Addresses ⇒ Response}
import responses.Addresses.Root
import utils.Slick.UpdateReturning._
import utils.jdbc.RecordNotUnique
import utils.jdbc.withUniqueConstraint

object AddressManager {
  def create(payload: CreateAddressPayload, customerId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Root Or Failure] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId)
    address.validate match {
      case Success ⇒
        db.run(for {
          newAddress ← Addresses.save(address)
          state ← States.findById(newAddress.stateId)
        } yield (newAddress, state)).map {
          case (address, Some(state)) ⇒ Good(Response.build(address, state))
          case (_, None)              ⇒ Bad(NotFoundFailure(State, address.stateId))
        }
      case f: Invalid ⇒ Future.successful(Bad(ValidationFailure(f)))
    }
  }

  def setDefaultShippingAddress(customerId: Int, addressId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Option[Failure]] = {
    db.run((for {
      _ ← Addresses._findDefaultByCustomerId(customerId).map(_.isDefaultShipping).update(false)
      newDefault ← Addresses._findById(addressId).extract.map(_.isDefaultShipping).update(true)
    } yield newDefault).transactionally).map { rows ⇒
      if (rows != 1) {
        Some(NotFoundFailure(Address, addressId))
      } else {
        None
      }
    }
  }

  /*
  def createFromPayload(customer: Customer, payload: Seq[CreateAddressPayload])
    (implicit ec: ExecutionContext, db: Database): Future[Seq[Address] Or Map[Address, Set[ErrorMessage]]] = {

    val addresses = payload.map(Address.fromPayload(_).copy(customerId = customer.id))
    create(customer, addresses)
  }

  def create(customer: Customer, addresses: Seq[Address])
    (implicit ec: ExecutionContext, db: Database): Future[Seq[Address] Or Map[Address, Set[ErrorMessage]]] = {

    val failures = addresses.map { a => (a, a.validate) }.filterNot { case (a, v) => v.isValid }

    if (failures.nonEmpty) {
      val acc = Map[Address, Set[ErrorMessage]]()
      val errorMap = failures.foldLeft(acc) { case (map, (address, failure)) =>
        map.updated(address, failure.messages)
      }
      Future.successful(Bad(errorMap))
    } else {
      db.run(for {
        _ <- this ++= addresses
        addresses <- filter(_.customerId === customer.id).result
      } yield Good(addresses))
    }
  }
  */
}
