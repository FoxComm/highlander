package services

import scala.concurrent.{ExecutionContext, Future}

import models.Order.RemorseHold
import models._
import org.scalactic._
import payloads.{CreateShippingAddress, GiftCardPayment, StoreCreditPayment, UpdateAddressPayload, UpdateShippingAddress}
import responses.{Addresses ⇒ Response, FullOrder}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import utils.Validation.Result.{Failure ⇒ Invalid, Success}

object OrderUpdater {

  def updateStatus(refNum: String, newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext): Future[FullOrder.Root Or Failures] = {

    updateStatuses(Seq(refNum), newStatus).flatMap {
      case Seq() ⇒ Orders.findByRefNum(refNum).result.run().flatMap(o ⇒ FullOrder.fromOrder(o.head).map(Good(_)))
      case failures: Failures ⇒ Future.successful(Bad(failures))
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

  final class NewRemorsePeriod(val remorsePeriod: Int)

  def increaseRemorsePeriod(order: Order)
    (implicit db: Database, ec: ExecutionContext): Future[NewRemorsePeriod Or Failures] = {
    order.status match {
      case RemorseHold ⇒
        val q = for {
          _ ← Orders.update(order.copy(remorsePeriodInMinutes = order.remorsePeriodInMinutes + 15))
          newOrder ← Orders._findById(order.id).result.headOption
        } yield newOrder
        db.run(q).map {
          case Some(newOrder) ⇒ Good(new NewRemorsePeriod(newOrder.remorsePeriodInMinutes))
          case None ⇒ Bad(List(GeneralFailure("Error during update")))
        }
      case _ ⇒ Future.successful(Bad(List(GeneralFailure("Order is not in RemorseHold status"))))
    }
  }

  def lock(order: Order, admin: StoreAdmin)
    (implicit db: Database, ec: ExecutionContext): Future[FullOrder.Root Or Failures] = {
    if (order.locked) {
      Future.successful(Bad(List(OrderLockedFailure(order.referenceNumber))))
    } else {
      val lock = Orders.update(order.copy(locked = true))
      val blame = OrderLockEvents += OrderLockEvent(orderId = order.id, lockedBy = admin.id)
      val queries = (lock >> blame).transactionally
      db.run(queries).flatMap { _ ⇒
        FullOrder.fromOrder(order).map(Good(_))
      }
    }
  }

  def unlock(order: Order)(implicit db: Database, ec: ExecutionContext): Future[FullOrder.Root Or Failures] = {
    if (order.locked) {
      val queries = for {
        _ ← Orders.update(order.copy(locked = false))
        newOrder ← Orders.findByRefNum(order.referenceNumber).result.head
      } yield newOrder

      db.run(queries).flatMap { o ⇒
        FullOrder.fromOrder(o).map(Good(_))
      }
    } else {
      Future.successful(Bad(List(GeneralFailure("Order is not locked"))))
    }
  }

  def createNote = "Note"

  def removeShippingAddress(orderId: Int)
    (implicit db: Database, ec: ExecutionContext): Future[Int] =
    db.run(OrderShippingAddresses.findByOrderId(orderId).delete)

  def createShippingAddress(order: Order, payload: CreateShippingAddress)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failures] = {

    (payload.addressId, payload.address) match {
      case (Some(addressId), _) ⇒
        createShippingAddressFromAddressId(addressId, order.id)
      case (None, Some(payloadAddress)) ⇒
        createShippingAddressFromPayload(Address.fromPayload(payloadAddress), order)
      case (None, None) ⇒
        Future.successful(Bad(List(GeneralFailure("must supply either an addressId or an address"))))
    }
  }

  def updateShippingAddress(order: Order, payload: UpdateShippingAddress)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failures] = {

    (payload.addressId, payload.address) match {
      case (Some(addressId), _) ⇒
        createShippingAddressFromAddressId(addressId, order.id)
      case (None, Some(address)) ⇒
        updateShippingAddressFromPayload(address, order)
      case (None, _) ⇒
        Future.successful(Bad(List(GeneralFailure("must supply either an addressId or an address"))))
    }

  }

  def addGiftCard(refNum: String, payload: GiftCardPayment)
    (implicit ec: ExecutionContext, db: Database): Future[OrderPayment Or Failure] = {
    db.run(for {
      order ← Orders.findByRefNum(refNum).result.headOption
      giftCard ← GiftCards.findByCode(payload.code).result.headOption
    } yield (order, giftCard)).flatMap {
      case (Some(order), Some(giftCard)) ⇒
        if (!giftCard.isActive) {
          Future.successful(Bad(GiftCardIsInactive(giftCard)))
        } else if (giftCard.hasAvailable(payload.amount)) {
          val payment = OrderPayment.build(giftCard).copy(orderId = order.id, amount = Some(payload.amount))
          OrderPayments.save(payment).run().map(Good(_))
        } else {
          Future.successful(Bad(GiftCardNotEnoughBalance(giftCard, payload.amount)))
        }
      case (None, _) ⇒
        Future.successful(Bad(OrderNotFoundFailure(refNum)))
      case (_, None) ⇒
        Future.successful(Bad(GiftCardNotFoundFailure(payload.code)))
    }
  }

  def addStoreCredit(refNum: String, payload: StoreCreditPayment)
    (implicit ec: ExecutionContext, db: Database): Future[Seq[OrderPayment] Or Failure] = {
    db.run(for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      storeCredits ← order.map { o ⇒
        StoreCredits.findAllActiveByCustomerId(o.customerId).result
      }.getOrElse(DBIO.successful(Seq.empty[StoreCredit]))
    } yield (order, storeCredits)).flatMap {
      case (Some(order), storeCredits) ⇒
        val available = storeCredits.map(_.availableBalance).sum

        if (available < payload.amount) {
          val error = CustomerHasInsufficientStoreCredit(id = order.customerId, has = available, want = payload.amount)
          Future.successful(Bad(error))
        } else {
          val payments = StoreCredit.processFifo(storeCredits.toList, payload.amount).map { case (sc, amount) ⇒
            OrderPayment.build(sc).copy(orderId = order.id, amount = Some(amount))
          }

          db.run(OrderPayments ++= payments).map { _ ⇒ Good(payments.toSeq) }
        }

      case (None, _) ⇒
        Future.successful(Bad(OrderNotFoundFailure(refNum)))
    }
  }

  def deletePayment(order: Order, paymentId: Int)
    (implicit ec: ExecutionContext, db: Database): Future[Int Or NotFoundFailure] = {
    db.run(OrderPayments.findAllByOrderId(order.id)
      .filter(_.paymentMethodId === paymentId)
      .delete).map { rowsAffected ⇒
      if (rowsAffected == 1) {
        Good(1)
      } else {
        Bad(NotFoundFailure(s"order payment method with id=$paymentId not found"))
      }
    }
  }

  def addCreditCard(refNum: String, id: Int)
    (implicit ec: ExecutionContext, db: Database): Future[OrderPayment Or Failure] = {
    db.run(for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      creditCard ← CreditCards._findById(id).result.headOption
      numCards ← order.map { o ⇒
        OrderPayments.findAllCreditCardsForOrder(o.id).length.result
      }.getOrElse(DBIO.successful(0))
    } yield (order, creditCard, numCards)).flatMap {
      case (Some(order), Some(creditCard), numCards) ⇒
        if (creditCard.isActive && numCards == 0) {
          val payment = OrderPayment.build(creditCard).copy(orderId = order.id, amount = None)
          OrderPayments.save(payment).run().map(Good(_))
        } else if (numCards > 0) {
          Future.successful(Bad(CartAlreadyHasCreditCard(order)))
        } else {
          Future.successful(Bad(CannotUseInactiveCreditCard(creditCard)))
        }
      case (None, _, _) ⇒
        Future.successful(Bad(OrderNotFoundFailure(refNum)))
      case (_, None, _) ⇒
        Future.successful(Bad(NotFoundFailure(CreditCard, id)))
    }
  }

  private def createShippingAddressFromPayload(address: Address, order: Order)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failures] = {

    address.validate match {
      case Success ⇒
        db.run(for {
          newAddress ← Addresses.save(address.copy(customerId = order.customerId))
          region ← Regions.findById(newAddress.regionId)
          _ ← OrderShippingAddresses.findByOrderId(order.id).delete
          _ ← OrderShippingAddresses.copyFromAddress(newAddress, order.id)
        } yield (newAddress, region)).map {
          case (address, Some(region))  ⇒ Good(Response.build(address, region))
          case (_, None)                ⇒ Bad(List(NotFoundFailure(Region, address.regionId)))
        }
      case f: Invalid ⇒ Future.successful(Bad(List(ValidationFailure(f))))
    }
  }

  private def updateShippingAddressFromPayload(payload: UpdateAddressPayload, order: Order)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failures] = {

    val actions = for {
      oldAddress ← OrderShippingAddresses.findByOrderId(order.id).result.headOption

      rowsAffected ← oldAddress.map { osa ⇒
        OrderShippingAddresses.update(OrderShippingAddress.fromPatchPayload(a = osa, p = payload))
      }.getOrElse(DBIO.successful(0))

      newAddress ← OrderShippingAddresses.findByOrderId(order.id).result.headOption

      region ← newAddress.map { address ⇒
        Regions.findById(address.regionId)
      }.getOrElse(DBIO.successful(None))
    } yield (rowsAffected, newAddress, region)

    db.run(actions.transactionally).map {
      case (_, None, _) ⇒
        Bad(List(NotFoundFailure(OrderShippingAddress, order.id)))
      case (0, _, _) ⇒
        Bad(List(GeneralFailure("Unable to update address")))
      case (_, Some(address), None) ⇒
        Bad(List(NotFoundFailure(Region, address.regionId)))
      case (_, Some(address), Some(region)) ⇒
        Good(Response.build(Address.fromOrderShippingAddress(address), region))
    }
  }

  private def createShippingAddressFromAddressId(addressId: Int, orderId: Int)
    (implicit db: Database, ec: ExecutionContext): Future[responses.Addresses.Root Or Failures] = {

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
    } yield (address, region)).map {
      case (Some(address), Some(region)) ⇒
        Good(Response.build(address, region))
      case _ ⇒
        Bad(List(NotFoundFailure(Address, addressId)))
    }
  }
}
