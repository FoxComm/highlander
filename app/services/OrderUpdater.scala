package services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import models.OrderLockEvents.scope._
import models._
import payloads.{CreateShippingAddress, UpdateAddressPayload, UpdateShippingAddress}
import responses.{Addresses ⇒ Response, FullOrder}
import slick.driver.PostgresDriver.api._
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object OrderUpdater {

  def updateStatus(refNum: String, newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor FullOrder.Root] = {

    updateStatuses(Seq(refNum), newStatus).flatMap {
      case Seq() ⇒ Orders.findByRefNum(refNum).result.run().flatMap(o ⇒ FullOrder.fromOrder(o.head).map(Xor.right))
      case failures: Failures ⇒ Result.failures(failures)
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
        .partition(_.transitionAllowed(newStatus))

      val (lockedOrders, absolutelyPossibleUpdates) = validTransitions.partition(_.locked)

      db.run(updateQueries(absolutelyPossibleUpdates.map(_.id))).map { _ ⇒
        // Failure handling
        val invalid = invalidTransitions.map { order ⇒
          OrderUpdateFailure(order.referenceNumber,
            s"Transition from ${order.status} to $newStatus is not allowed")
        }
        val notFound = refNumbers
          .filterNot(refNum ⇒ orders.map(_.referenceNumber).contains(refNum))
          .map(refNum ⇒ OrderUpdateFailure(refNum, "Not found"))
        val locked = lockedOrders.map { order ⇒
          OrderUpdateFailure(order.referenceNumber, "Order is locked")
        }

        invalid ++ notFound ++ locked
      }
    }
  }

  def lock(order: Order, admin: StoreAdmin)
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor FullOrder.Root] = {
    if (order.locked) {
      Result.failures(List(OrderLockedFailure(order.referenceNumber)))
    } else {
      val lock = Orders.update(order.copy(locked = true))
      val blame = OrderLockEvents += OrderLockEvent(orderId = order.id, lockedBy = admin.id)
      val queries = (lock >> blame).transactionally
      db.run(queries).flatMap { _ ⇒
        FullOrder.fromOrder(order).map(Xor.right)
      }
    }
  }

  private def newRemorseEnd(maybeRemorseEnd: Option[Instant], lockedAt: Instant): Option[Instant] = {
    maybeRemorseEnd.map(_.plusMillis(Instant.now.toEpochMilli - lockedAt.toEpochMilli))
  }

  private def updateUnlock(orderId: Int, remorseEnd: Option[Instant])
    (implicit db: Database) = {
    Orders._findById(orderId).extract
      .map { o ⇒ (o.locked, o.remorsePeriodEnd) }
      .updateReturning(Orders.map(identity), (false, remorseEnd))
  }

  def unlock(order: Order)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    if (order.locked) {
      val queries = OrderLockEvents.findByOrder(order).mostRecentLock.result.headOption.flatMap {
        case Some(lockEvent) ⇒
          updateUnlock(order.id, newRemorseEnd(order.remorsePeriodEnd, lockEvent.lockedAt))
        case None ⇒
          updateUnlock(order.id, order.remorsePeriodEnd.map(_.plusSeconds(15 * 60)))
      }
      db.run(queries).flatMap { o ⇒
        Result.fromFuture(FullOrder.fromOrder(o.head))
      }
    } else Result.failure(GeneralFailure("Order is not locked"))
  }

  def createNote = "Note"

  def removeShippingAddress(orderId: Int)
    (implicit db: Database, ec: ExecutionContext): Future[Int] =
    db.run(OrderShippingAddresses.findByOrderId(orderId).delete)

  def createShippingAddress(order: Order, payload: CreateShippingAddress)
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor responses.Addresses.Root] = {

    (payload.addressId, payload.address) match {
      case (Some(addressId), _) ⇒
        createShippingAddressFromAddressId(addressId, order.id)
      case (None, Some(payloadAddress)) ⇒
        createShippingAddressFromPayload(Address.fromPayload(payloadAddress), order)
      case (None, None) ⇒
        Result.failure(GeneralFailure("must supply either an addressId or an address"))
    }
  }

  def updateShippingAddress(order: Order, payload: UpdateShippingAddress)
    (implicit db: Database, ec: ExecutionContext): Result[responses.Addresses.Root] = {

    (payload.addressId, payload.address) match {
      case (Some(addressId), _) ⇒
        createShippingAddressFromAddressId(addressId, order.id)
      case (None, Some(address)) ⇒
        updateShippingAddressFromPayload(address, order)
      case (None, _) ⇒
        Result.failure(GeneralFailure("must supply either an addressId or an address"))
    }
  }

  private def createShippingAddressFromPayload(address: Address, order: Order)
    (implicit db: Database, ec: ExecutionContext): Result[responses.Addresses.Root] = {

    address.validate match {
      case Valid(_) ⇒
        db.run(for {
          newAddress ← Addresses.save(address.copy(customerId = order.customerId))
          region ← Regions.findById(newAddress.regionId)
          _ ← OrderShippingAddresses.findByOrderId(order.id).delete
          _ ← OrderShippingAddresses.copyFromAddress(newAddress, order.id)
        } yield (newAddress, region)).flatMap {
          case (address, Some(region))  ⇒ Result.good(Response.build(address, region))
          case (_, None)                ⇒ Result.failure(NotFoundFailure(Region, address.regionId))
        }
      case Invalid(err) ⇒ Result.failure(err.head)
    }
  }

  private def updateShippingAddressFromPayload(payload: UpdateAddressPayload, order: Order)
    (implicit db: Database, ec: ExecutionContext): Result[responses.Addresses.Root] = {

    val actions = for {
      oldAddress ← OrderShippingAddresses.findByOrderId(order.id).one

      rowsAffected ← oldAddress.map { osa ⇒
        OrderShippingAddresses.update(OrderShippingAddress.fromPatchPayload(a = osa, p = payload))
      }.getOrElse(DBIO.successful(0))

      newAddress ← OrderShippingAddresses.findByOrderId(order.id).one

      region ← newAddress.map { address ⇒
        Regions.findById(address.regionId)
      }.getOrElse(DBIO.successful(None))
    } yield (rowsAffected, newAddress, region)

    db.run(actions.transactionally).flatMap {
      case (_, None, _) ⇒
        Result.failure(NotFoundFailure(OrderShippingAddress, order.id))
      case (0, _, _) ⇒
        Result.failure(GeneralFailure("Unable to update address"))
      case (_, Some(address), None) ⇒
        Result.failure(NotFoundFailure(Region, address.regionId))
      case (_, Some(address), Some(region)) ⇒
        Result.right(Response.build(Address.fromOrderShippingAddress(address), region))
    }
  }

  private def createShippingAddressFromAddressId(addressId: Int, orderId: Int)
    (implicit db: Database, ec: ExecutionContext): Result[responses.Addresses.Root] = {

    db.run(for {
      address ← Addresses.findById(addressId)
      region ← address.map { a ⇒ Regions.findById(a.regionId) }.getOrElse(DBIO.successful(None))
      _ ← address match {
        case Some(a) ⇒
          for {
            _ ← OrderShippingAddresses.findByOrderId(orderId).delete
            shipAddress ← OrderShippingAddresses.copyFromAddress(a, orderId)
          } yield Some(shipAddress)

        case None ⇒
          DBIO.successful(None)
      }
    } yield (address, region)).flatMap {
      case (Some(address), Some(region)) ⇒
        Result.good(Response.build(address, region))
      case _ ⇒
        Result.failure(NotFoundFailure(Address, addressId))
    }
  }
}
