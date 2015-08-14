package services

import models._
import payloads.{UpdateAddressPayload, BulkUpdateOrdersPayload, CreateShippingAddress, UpdateShippingAddress,
UpdateOrderPayload}
import slick.dbio
import slick.dbio.Effect.{Transactional, Write}
import utils.Http._
import utils.TableQueryWithId

import utils.Validation.Result.{Failure ⇒ Invalid, Success}
import org.scalactic._
import scala.concurrent.{Future, ExecutionContext}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import responses.{Addresses ⇒ Response, FullOrder}

object OrderUpdater {

  def updateStatus(refNum: String, finder: Query[Orders, Order, Seq], newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext): Future[FullOrder.Root Or Failure] = {

    updateStatuses(Seq(refNum), newStatus).flatMap {
      case Seq(failure) ⇒ Future.successful(Bad(failure))
      case Seq() ⇒ finder.result.run().flatMap(o ⇒ FullOrder.fromOrder(o.head).map(Good(_)))
    }
  }

  def updateStatuses(refNumbers: Seq[String], newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext): Future[Seq[OrderUpdateFailure]] = {

    import Order._

    def cancelOrders(orderIds: Seq[Int]) = {
      val updateLineItems = OrderLineItems
        .filter(_.orderId.inSetBind(orderIds))
        .map(_.status)
        .update(OrderLineItem.Canceled)

      // TODO: canceling an order must cascade to status on each payment type not order_payments
//      val updateOrderPayments = OrderPayments
//        .filter(_.orderId.inSetBind(orderIds))
//        .map(_.status)
//        .update("cancelAuth")

      val updateOrder = Orders.filter(_.id.inSetBind(orderIds)).map(_.status).update(newStatus)

      // (updateLineItems >> updateOrderPayments >> updateOrder).transactionally
      (updateLineItems >> updateOrder).transactionally
    }

    def updateQueries(orderIds: Seq[Int]) = newStatus match {
      case Canceled ⇒ cancelOrders(orderIds)
      case _ ⇒ Orders.filter(_.id.inSet(orderIds)).map(_.status).update(newStatus)
    }

    db.run(Orders.filter(_.referenceNumber.inSet(refNumbers)).result).flatMap { orders ⇒

      val (validTransitions, invalidTransitions) = orders
        .filterNot(_.status == newStatus)
        .partition(o ⇒ transitionAllowed(o.status, newStatus))

      db.run(updateQueries(validTransitions.map(_.id))).map { _ ⇒
        // Failure handling
        val invalid = invalidTransitions.map { order ⇒
          OrderUpdateFailure(order.referenceNumber,
            s"Transition from ${order.status} to $newStatus is not allowed")
        }
        val notFound = refNumbers
          .filterNot(refNum ⇒ orders.map(_.referenceNumber).contains(refNum))
          .map(refNum ⇒ OrderUpdateFailure(refNum, "Not found"))

        invalid ++ notFound
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

  def updateShippingAddress(order: Order, payload: UpdateShippingAddress)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failure] = {

    (payload.addressId, payload.address) match {
      case (Some(addressId), _) ⇒
        createShippingAddressFromAddressId(addressId, order.id)
      case (None, Some(address)) ⇒
        updateShippingAddressFromPayload(address, order)
      case (None, _) ⇒
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

  private def updateShippingAddressFromPayload(payload: UpdateAddressPayload, order: Order)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failure] = {

    val actions = for {
      oldAddress ← OrderShippingAddresses.findByOrderId(order.id).result.headOption

      rowsAffected ← oldAddress.map { osa ⇒
        OrderShippingAddresses.update(OrderShippingAddress.fromPatchPayload(a = osa, p = payload))
      }.getOrElse(DBIO.successful(0))

      newAddress ← OrderShippingAddresses.findByOrderId(order.id).result.headOption

      state ← newAddress.map { address ⇒
        States.findById(address.stateId)
      }.getOrElse(DBIO.successful(None))
    } yield (rowsAffected, newAddress, state)

    db.run(actions.transactionally).map {
      case (_, None, _) ⇒
        Bad(NotFoundFailure(OrderShippingAddress, order.id))
      case (0, _, _) ⇒
        Bad(GeneralFailure("Unable to update address"))
      case (_, Some(address), None) ⇒
        Bad(NotFoundFailure(State, address.stateId))
      case (_, Some(address), Some(state)) ⇒
        Good(Response.build(Address.fromOrderShippingAddress(address), state))
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
