package services

import scala.concurrent.{ExecutionContext, Future}

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import models._
import payloads.{UpdateShippingMethod, CreateAddressPayload, UpdateAddressPayload}
import responses.FullOrder
import slick.driver.PostgresDriver.api._
import utils.Slick.{DbResult, _}
import utils.Slick.UpdateReturning._
import utils.Slick.implicits._

object OrderUpdater {

  def updateStatus(refNum: String, newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext): Future[Failures Xor FullOrder.Root] = {

    updateStatuses(Seq(refNum), newStatus).flatMap {
      case Seq() ⇒ Orders.findByRefNum(refNum).result.run().flatMap { o ⇒
        FullOrder.fromOrder(o.head).run().map(Xor.right)
      }
      case failures ⇒ Result.failures(failures: _*)
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

  def removeShippingAddress(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
   val finder = Orders.findByRefNum(refNum)
   finder.selectOne {
       order ⇒  
         OrderShippingAddresses.findByOrderId(order.id).delete.flatMap {
           case 1 ⇒  DbResult.fromDbio(fullOrder(finder))
           case 0 ⇒  DbResult.failure(NotFoundFailure(s"Shipping Address for order with reference number ${refNum} not found"))
         }
     }
  }

  def createShippingAddressFromPayload(payload: CreateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒

      val address = Address.fromPayload(payload)
      address.validate match {
        case Valid(_) ⇒
          (for {
            newAddress ← Addresses.save(address.copy(customerId = order.customerId))
            region ← Regions.findOneById(newAddress.regionId)
            _ ← OrderShippingAddresses.findByOrderId(order.id).delete
            _ ← OrderShippingAddresses.copyFromAddress(newAddress, order.id)
          } yield region).flatMap {
            case Some(region) ⇒ DbResult.fromDbio(fullOrder(finder))
            case None ⇒ DbResult.failure(NotFoundFailure(Region, address.regionId))
          }
        case Invalid(errors) ⇒ DbResult.failures(errors)
      }
    }
  }

  def updateShippingAddressFromPayload(payload: UpdateAddressPayload, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒

      val actions = for {
        oldAddress ← OrderShippingAddresses.findByOrderId(order.id).one

        rowsAffected ← oldAddress.map { osa ⇒
          OrderShippingAddresses.update(OrderShippingAddress.fromPatchPayload(a = osa, p = payload))
        }.getOrElse(lift(0))

        newAddress ← OrderShippingAddresses.findByOrderId(order.id).one

        region ← newAddress.map { address ⇒
          Regions.findOneById(address.regionId)
        }.getOrElse(lift(None))
      } yield (rowsAffected, newAddress, region)

      actions.flatMap {
        case (_, None, _) ⇒
          DbResult.failure(NotFoundFailure(OrderShippingAddress, order.id))
        case (0, _, _) ⇒
          DbResult.failure(GeneralFailure("Unable to update address"))
        case (_, Some(address), None) ⇒
          DbResult.failure(NotFoundFailure(Region, address.regionId))
        case (_, Some(address), Some(region)) ⇒
          DbResult.fromDbio(fullOrder(finder))
      }
    }
  }

  def updateShippingMethod(payload: UpdateShippingMethod, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = ShippingMethods.findActiveById(payload.shippingMethodId)

    finder.findOneAndRun { shippingMethod ⇒
      val order = Orders.findByRefNum(refNum).one.run()
      OrderShippingMethods.copyFromShippingMethod(shippingMethod, order).run()

      Orders.findById(order.id).flatMap {
        case Some(o) ⇒
          DbResult.fromDbio(FullOrder.fromOrder(o))
        case _ ⇒
          DbResult.failure(GeneralFailure("Some stupid failure"))
      }
    }
  }

  def createShippingAddressFromAddressId(addressId: Int, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {

    val finder = Orders.findByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒

      (for {
        address ← Addresses.findOneById(addressId)
        region ← address.map { a ⇒ Regions.findOneById(a.regionId) }.getOrElse(DBIO.successful(None))
        _ ← address match {
          case Some(a) ⇒
            for {
              _ ← OrderShippingAddresses.findByOrderId(order.id).delete
              shipAddress ← OrderShippingAddresses.copyFromAddress(a, order.id)
            } yield Some(shipAddress)

          case None ⇒
            DBIO.successful(None)
        }
      } yield (address, region)).flatMap {
        case (Some(address), Some(region)) ⇒
          DbResult.fromDbio(fullOrder(finder))
        case _ ⇒
          DbResult.failure(NotFoundFailure(Address, addressId))
      }
    }
  }
}
