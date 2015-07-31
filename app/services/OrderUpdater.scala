package services

import models._
import payloads.{CreateShippingAddress, UpdateOrderPayload}
import slick.dbio.Effect.All

import utils.Validation.Result.{Failure ⇒ Invalid, Success}
import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import responses.{Addresses ⇒ Response}

object OrderUpdater {

  def updateStatus(order: Order, payLoad: UpdateOrderPayload)
                  (implicit db: Database, ec: ExecutionContext): Future[Order Or List[ErrorMessage]]  = {
    val newOrder = order.copy(status = Order.FulfillmentStarted)
    val insertedQuery = for {
      _ <- Orders.insertOrUpdate(newOrder)
      updatedOrder <- Orders.findById(order.id)
    } yield (updatedOrder)

    db.run(insertedQuery).map { optOrder =>
      optOrder match {
        case Some(orderExists) => Good(orderExists)
        case None => Bad(List("Not able to update order"))
      }
    }

  }

  def createNote = "Note"

  def removeShippingAddress(orderId: Int)
    (implicit db: Database, ec: ExecutionContext): Future[Int] =
    db.run(OrderShippingAddresses.findByOrderId(orderId).delete)

  def createShippingAddress(order: Order, payload: CreateShippingAddress)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failure] = {

    (payload.addressId, payload.address) match {
      case (Some(addressId), _) ⇒
        createShippingAddressFromAddressId(addressId, order.id)
      case (None, Some(payloadAddress)) ⇒
        createShippingAddressFromPayload(Address.fromPayload(payloadAddress), order)
      case (None, None) ⇒
        Future.successful(Bad(GeneralFailure("must supply either an addressId or an address")))
    }
  }

  private def createShippingAddressFromPayload(address: Address, order: Order)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failure] = {

    address.validate match {
      case Success ⇒
        db.run(for {
          newAddress ← Addresses.save(address.copy(customerId = order.customerId))
          state ← States.findById(newAddress.stateId)
          _ ← OrderShippingAddresses.copyFromAddress(newAddress, order.id)
        } yield (newAddress, state)).map {
          case (address, Some(state)) ⇒ Good(Response.build(address, state))
          case (_, None)              ⇒ Bad(NotFoundFailure(State, address.stateId))
        }
      case f: Invalid ⇒ Future.successful(Bad(ValidationFailure(f)))
    }
  }

  private def createShippingAddressFromAddressId(addressId: Int, orderId: Int)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failure] = {

    db.run(for {
      address ← Addresses.findById(addressId)
      state ← address.map { a ⇒ States.findById(a.stateId) }.getOrElse(DBIO.successful(None))
      _ ← address.map(OrderShippingAddresses.copyFromAddress(_, orderId).map(Some(_))).
        getOrElse(DBIO.successful(None))
    } yield (address, state)).map {
      case (Some(address), Some(state)) ⇒
        Good(Response.build(address, state))
      case _ ⇒
        Bad(NotFoundFailure(Address, addressId))
    }
  }
}
