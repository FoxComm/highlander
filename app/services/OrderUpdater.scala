package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{Valid, Invalid}
import cats.data.Xor
import models.Order.RemorseHold
import models._

import payloads.{CreateShippingAddress, GiftCardPayment, StoreCreditPayment, UpdateAddressPayload, UpdateShippingAddress}
import responses.{Addresses ⇒ Response, FullOrder}
import slick.dbio.Effect.Write
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef ⇒ Database}
import slick.profile.FixedSqlAction

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

  final class NewRemorsePeriod(val remorsePeriod: Int)

  def increaseRemorsePeriod(order: Order)
    (implicit db: Database, ec: ExecutionContext): Result[NewRemorsePeriod] = {
    order.status match {
      case RemorseHold ⇒
        val q = for {
          _        ← Orders.update(order.copy(remorsePeriodInMinutes = order.remorsePeriodInMinutes + 15))
          newOrder ← Orders._findById(order.id).result.headOption
        } yield newOrder

        db.run(q).flatMap {
          case Some(newOrder) ⇒ Result.good(new NewRemorsePeriod(newOrder.remorsePeriodInMinutes))
          case None           ⇒ Result.failure(GeneralFailure("Error during update"))
        }

      case _ ⇒ Result.failure(GeneralFailure("Order is not in RemorseHold status"))
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

  def unlock(order: Order)(implicit db: Database, ec: ExecutionContext): Future[Failures Xor FullOrder.Root]
  = {
    if (order.locked) {
      val queries = for {
        _ ← Orders.update(order.copy(locked = false))
        newOrder ← Orders.findByRefNum(order.referenceNumber).result.head
      } yield newOrder

      db.run(queries).flatMap { o ⇒
        FullOrder.fromOrder(o).map(Xor.right)
      }
    } else {
      Future.successful(Xor.left(List(GeneralFailure("Order is not locked"))))
    }
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
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor responses.Addresses.Root] = {

    (payload.addressId, payload.address) match {
      case (Some(addressId), _) ⇒
        createShippingAddressFromAddressId(addressId, order.id)
      case (None, Some(address)) ⇒
        updateShippingAddressFromPayload(address, order)
      case (None, _) ⇒
        Future.successful(Xor.left(List(GeneralFailure("must supply either an addressId or an address"))))
    }
  }

  def addGiftCard(refNum: String, payload: GiftCardPayment)
    (implicit ec: ExecutionContext, db: Database): Result[OrderPayment] = {
    db.run(for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      giftCard ← GiftCards.findByCode(payload.code).result.headOption
    } yield (order, giftCard)).flatMap {
      case (Some(order), Some(giftCard)) ⇒
        if (!giftCard.isActive) {
          Result.left(GiftCardIsInactive(giftCard))
        } else if (giftCard.hasAvailable(payload.amount)) {
          val payment = OrderPayment.build(giftCard).copy(orderId = order.id, amount = Some(payload.amount))
          OrderPayments.save(payment).run().map(Xor.right)
        } else {
          Result.left(GiftCardNotEnoughBalance(giftCard, payload.amount))
        }
      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum))
      case (_, None) ⇒
        Result.left(GiftCardNotFoundFailure(payload.code))
    }
  }

  def addStoreCredit(refNum: String, payload: StoreCreditPayment)
    (implicit ec: ExecutionContext, db: Database): Result[Seq[OrderPayment]] = {
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
          Result.left(error)
        } else {
          val payments = StoreCredit.processFifo(storeCredits.toList, payload.amount).map { case (sc, amount) ⇒
            OrderPayment.build(sc).copy(orderId = order.id, amount = Some(amount))
          }

          db.run(OrderPayments ++= payments).map { _ ⇒ Xor.right(payments.toSeq) }
        }

      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum))
    }
  }

  def addCreditCard(refNum: String, id: Int)
    (implicit ec: ExecutionContext, db: Database): Result[OrderPayment] = {
    val actions = for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      creditCard ← CreditCards._findById(id).result.headOption
    } yield (order, creditCard)

    actions.run().flatMap {
      case (Some(order), Some(creditCard)) ⇒
        if (creditCard.isActive) {
          val payment = OrderPayment.build(creditCard).copy(orderId = order.id, amount = None)
          val delete = OrderPayments.creditCards.filter(_.orderId === order.id).delete
          val replaceOrCreate = OrderPayments.save(payment)

          (delete >> replaceOrCreate).transactionally.run().map(Xor.right)
        } else {
          Result.left(CannotUseInactiveCreditCard(creditCard))
        }

      case (None, _) ⇒
        Result.left(OrderNotFoundFailure(refNum))

      case (_, None) ⇒
        Result.left(NotFoundFailure(CreditCard, id))
    }
  }

  def deleteCreditCard(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Unit] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.CreditCard)

  def deleteStoreCredit(refNum: String)(implicit ec: ExecutionContext, db: Database): Result[Unit] =
    deleteCreditCardOrStoreCredit(refNum, PaymentMethod.StoreCredit)

  private def deleteCreditCardOrStoreCredit(refNum: String, pmt: PaymentMethod.Type)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val actions = for {
      order ← Orders.findCartByRefNum(refNum).result.headOption
      payments ← order.map { o ⇒
        OrderPayments.byType(pmt).filter(_.orderId === o.id).delete
      }.getOrElse(DBIO.successful(0))
    } yield (order, payments)

    db.run(actions.transactionally).flatMap {
      case (None, _)        ⇒ Result.failure(OrderNotFoundFailure(refNum))
      case (Some(order), 0) ⇒ Result.failure(OrderPaymentNotFoundFailure(pmt))
      case (Some(order), _) ⇒ Result.good({})
    }
  }

  def deleteGiftCard(refNum: String, code: String)
    (implicit ec: ExecutionContext, db: Database): Result[Unit] = {

    val finders = for {
      order     ← Orders.findCartByRefNum(refNum).result.headOption
      giftCard  ← GiftCards.findByCode(code).result.headOption
    } yield (order, giftCard)

    def deletePayment(f: (Option[Order], Option[GiftCard])): DBIO[Failures Xor Unit] = f match {
      case (Some(order), Some(giftCard)) ⇒
        OrderPayments.giftCards.filter(_.paymentMethodId === giftCard.id)
          .filter(_.orderId === order.id).delete.map { rows ⇒
          if (rows == 1) Xor.right({}) else Xor.left(OrderPaymentNotFoundFailure(GiftCard).single)
        }

      case (None, _) ⇒
        DBIO.successful(Xor.left(OrderNotFoundFailure(refNum).single))

      case (_, None) ⇒
        DBIO.successful(Xor.left(GiftCardNotFoundFailure(code).single))
    }

    db.run(finders.flatMap(deletePayment).transactionally)
  }

  private def createShippingAddressFromPayload(address: Address, order: Order)
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor responses.Addresses.Root] = {

    address.validateNew match {
      case Valid(_) ⇒
        db.run(for {
          newAddress ← Addresses.save(address.copy(customerId = order.customerId))
          region ← Regions.findById(newAddress.regionId)
          _ ← OrderShippingAddresses.findByOrderId(order.id).delete
          _ ← OrderShippingAddresses.copyFromAddress(newAddress, order.id)
        } yield (newAddress, region)).map {
          case (address, Some(region))  ⇒ Xor.right(Response.build(address, region))
          case (_, None)                ⇒ Xor.left(List(NotFoundFailure(Region, address.regionId)))
        }
      case Invalid(err) ⇒ Future.successful(Xor.left(List(ValidationFailureNew(err))))
    }
  }

  private def updateShippingAddressFromPayload(payload: UpdateAddressPayload, order: Order)
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor responses.Addresses.Root] = {

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
        Xor.left(List(NotFoundFailure(OrderShippingAddress, order.id)))
      case (0, _, _) ⇒
        Xor.left(List(GeneralFailure("Unable to update address")))
      case (_, Some(address), None) ⇒
        Xor.left(List(NotFoundFailure(Region, address.regionId)))
      case (_, Some(address), Some(region)) ⇒
        Xor.right(Response.build(Address.fromOrderShippingAddress(address), region))
    }
  }

  private def createShippingAddressFromAddressId(addressId: Int, orderId: Int)
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor responses.Addresses.Root] = {

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
        Xor.right(Response.build(address, region))
      case _ ⇒
        Xor.left(List(NotFoundFailure(Address, addressId)))
    }
  }
}
