package services

import models._
import payloads.{BulkUpdateOrdersPayload, CreateShippingAddress, UpdateOrderPayload}
import utils.Http._

import utils.Validation.Result.{Failure ⇒ Invalid, Success}
import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import responses.{Addresses ⇒ Response, FullOrder}

object OrderUpdater {

  def updateSingleOrder(order: Order, finder: Query[Orders, Order, Seq], payload: UpdateOrderPayload)
    (implicit db: Database, ec: ExecutionContext): Future[FullOrder.Root Or Failure]  = {

    def unexpectedError = Bad(OrderUpdateFailure(order.referenceNumber, "This was really unexpected"))

    OrderUpdater.updateStatus(order, payload).flatMap {
      case Some(failure) ⇒ Future.successful(Bad(failure))
      case None ⇒ finder.result.headOption.run().flatMap {
        case Some(newOrder) ⇒ FullOrder.fromOrder(newOrder).map(_.get).map(Good(_))
        case None ⇒ Future.successful(unexpectedError)
      }
    }
  }

  def updateMultipleOrders (payload: BulkUpdateOrdersPayload)
    (implicit db: Database, ec: ExecutionContext): Future[Seq[OrderUpdateFailure]] = {
    Future.sequence(payload.referenceNumbers.map {
      refNum ⇒
        Orders.findByRefNum(refNum).result.headOption.run().flatMap {
          case Some(order) ⇒ OrderUpdater.updateStatus(order, UpdateOrderPayload(payload.status))
          case None ⇒ Future.successful(Some(OrderUpdateFailure(refNum, "Not found")))
        }
    }).map(_.flatMap(f ⇒ f))
  }

  def updateStatus(order: Order, payLoad: UpdateOrderPayload)
    (implicit db: Database, ec: ExecutionContext): Future[Option[OrderUpdateFailure]] = {

    import Order._

    def update(newStatus: Status) = {
      val newOrder = order.copy(status = newStatus)
      val insertedQuery = for {
        _ ← Orders.insertOrUpdate(newOrder)
        updatedOrder ← Orders.findById(order.id)
      } yield updatedOrder

      db.run(insertedQuery).map {
        case Some(orderExists) ⇒ None
        case None ⇒ Some(OrderUpdateFailure(order.referenceNumber, "Not able to update order"))
      }
    }

    def fail(s: String) = Future.successful(Some(OrderUpdateFailure(order.referenceNumber, s)))

    val newStatus = payLoad.status
    val currentStatus = order.status

    if (newStatus == currentStatus) {
      Future.successful(None)
    } else {
      allowedStateTransitions.get(currentStatus) match {
        case Some(allowed) ⇒
          if (allowed.contains(newStatus)) {
            update(newStatus)
          } else {
            fail(s"Transition from $currentStatus to $newStatus is not allowed")
          }
        case None ⇒ fail(s"Transition from current status $currentStatus is not allowed")
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
          _ ← OrderShippingAddresses.findByOrderId(order.id).delete
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
      _ ← address match {
        case Some(a) ⇒
          for {
            _ ← OrderShippingAddresses.findByOrderId(orderId).delete
            shipAddress ← OrderShippingAddresses.copyFromAddress(a, orderId)
          } yield Some(shipAddress)
        case None ⇒
          DBIO.successful(None)
      }
    } yield (address, state)).map {
      case (Some(address), Some(state)) ⇒
        Good(Response.build(address, state))
      case _ ⇒
        Bad(NotFoundFailure(Address, addressId))
    }
  }
}
