package services

import scala.concurrent.ExecutionContext

import cats.data.Validated.{Invalid, Valid}
import cats.data.Xor
import cats.data.Xor.{Left, Right}
import models._
import payloads.{UpdateShippingMethod, CreateAddressPayload, UpdateAddressPayload}
import responses.ResponseWithFailuresAndMetadata.BulkOrderUpdateResponse
import responses.{ResponseWithFailuresAndMetadata, FullOrder}
import slick.driver.PostgresDriver.api._
import utils.CustomDirectives
import utils.CustomDirectives.SortAndPage
import utils.Slick.{DbResult, _}
import utils.Slick.implicits._

object OrderUpdater {

  def updateStatus(refNum: String, newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)
    finder.selectOneForUpdate { order ⇒
      updateStatusesDbio(Seq(refNum), newStatus).flatMap {
        case Xor.Right(_) ⇒
          DbResult.fromDbio(fullOrder(finder))
        case Xor.Left(failures) ⇒
          val fs = failures.toList.map {
            case NotFoundFailure400(msg) ⇒ NotFoundFailure404(msg)
            case anyOtherFailure         ⇒ anyOtherFailure
          }
          DbResult.failures(Failures(fs: _*))
      }
    }
  }

  def updateStatuses(refNumbers: Seq[String], newStatus: Order.Status)
    (implicit db: Database, ec: ExecutionContext, sortAndPage: SortAndPage): Result[BulkOrderUpdateResponse] = {
    updateStatusesDbio(refNumbers, newStatus).zip(OrderQueries.findAll).map { case (failures,
    orders) ⇒
      ResponseWithFailuresAndMetadata.fromOption(orders, failures.swap.toOption)
    }.transactionally.run().flatMap(Result.good)
  }

  private def updateStatusesDbio(refNumbers: Seq[String], newStatus: Order.Status)(implicit db: Database,
    ec: ExecutionContext, sortAndPage: SortAndPage = CustomDirectives.EmptySortAndPage): DbResult[Unit] = {

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

    val query = Orders.filter(_.referenceNumber.inSet(refNumbers)).result
    appendForUpdate(query).flatMap { orders ⇒

      val (validTransitions, invalidTransitions) = orders
        .filterNot(_.status == newStatus)
        .partition(_.transitionAllowed(newStatus))

      val (lockedOrders, absolutelyPossibleUpdates) = validTransitions.partition(_.locked)

      updateQueries(absolutelyPossibleUpdates.map(_.id)).flatMap { _ ⇒
        // Failure handling
        val invalid = invalidTransitions.map { order ⇒
          OrderStatusTransitionNotAllowed(order.status, newStatus, order.refNum)
        }
        val notFound = refNumbers
          .filterNot(refNum ⇒ orders.map(_.referenceNumber).contains(refNum))
          .map(refNum ⇒ NotFoundFailure400(Order, refNum))
        val locked = lockedOrders.map { order ⇒ LockedFailure(Order, order.refNum) }

        val failures = invalid ++ notFound ++ locked
        if (failures.isEmpty) DbResult.unit else DbResult.failures(Failures(failures: _*))
      }
    }
  }

  def removeShippingAddress(refNum: String)(implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOne ({ order ⇒
      OrderShippingAddresses.findByOrderId(order.id).delete.flatMap {
        case 1 ⇒
          DbResult.fromDbio(fullOrder(finder))
        case 0 ⇒
          DbResult.failure(NotFoundFailure400(s"Shipping Address for order with reference number $refNum not found"))
      }
    }, checks = finder.checks + finder.mustBeCart)
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
            case None ⇒ DbResult.failure(NotFoundFailure404(Region, address.regionId))
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
          DbResult.failure(NotFoundFailure404(OrderShippingAddress, order.id))
        case (0, _, _) ⇒
          DbResult.failure(GeneralFailure("Unable to update address"))
        case (_, Some(address), None) ⇒
          DbResult.failure(NotFoundFailure404(Region, address.regionId))
        case (_, Some(address), Some(region)) ⇒
          DbResult.fromDbio(fullOrder(finder))
      }
    }
  }

  def updateShippingMethod(payload: UpdateShippingMethod, refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdate { order ⇒
      ShippingMethods.findActiveById(payload.shippingMethodId).one.flatMap {
        case Some(shippingMethod) ⇒
          ShippingManager.evaluateShippingMethodForOrder(shippingMethod, order).flatMap {
            case Right(res) ⇒
              if (res) {
                val orderShipping = OrderShippingMethod(orderId = order.id, shippingMethodId = shippingMethod.id)
                val delete = OrderShippingMethods.findByOrderId(order.id).delete

                DbResult.fromDbio(delete >> OrderShippingMethods.save(orderShipping) >> fullOrder(finder))
              } else {
                DbResult.failure(ShippingMethodNotApplicableToOrder(payload.shippingMethodId, order.refNum))
              }
            case Left(f) ⇒
              DbResult.failures(f)
          }
        case None ⇒
          DbResult.failure(ShippingMethodDoesNotExist(payload.shippingMethodId))
      }
    }
  }

  def deleteShippingMethod(refNum: String)
    (implicit db: Database, ec: ExecutionContext): Result[FullOrder.Root] = {
    val finder = Orders.findByRefNum(refNum)

    finder.selectOneForUpdate { order ⇒
      DbResult.fromDbio(OrderShippingMethods.findByOrderId(order.id).delete >> fullOrder(finder))
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
          DbResult.failure(NotFoundFailure404(Address, addressId))
      }
    }
  }
}
