package services

import scala.concurrent.{ExecutionContext, Future}

import org.scalactic.{Or, Good, Bad}
import utils.Validation.Result.{Failure ⇒ Invalid, Success}
import models.{Addresses ⇒ Table, Address, States, State, Customer}
import payloads.CreateAddressPayload
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import responses.{Addresses ⇒ Response}
import responses.Addresses.Root

object AddressManager {
  def createOne(customerId: Int, payload: CreateAddressPayload)
    (implicit ec: ExecutionContext, db: Database): Future[Root Or Failure] = {
    val address = Address.fromPayload(payload).copy(customerId = customerId)
    address.validate match {
      case Success ⇒
        db.run(for {
          newAddress ← Table.save(address)
          state ← States.findById(newAddress.stateId).result.headOption
        } yield (newAddress, state)).map {
          case (address, Some(state)) ⇒ Good(Response.build(address, state))
          case (_, None)              ⇒ Bad(NotFoundFailure(State, address.stateId))
        }
      case f: Invalid ⇒ Future.successful(Bad(ValidationFailure(f)))
    }
  }
}
