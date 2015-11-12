package services.orders

import scala.concurrent.ExecutionContext

import cats.data.Xor.{Left, Right}
import models._
import payloads.UpdateShippingMethod
import responses.FullOrder
import services._
import slick.driver.PostgresDriver.api._
import utils.Slick.implicits._
import utils.Slick.{DbResult, _}

object OrderShippingMethodUpdater {

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

                DbResult.fromDbio(for {
                  deleteShipping ← Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(None)
                  delete ← OrderShippingMethods.findByOrderId(order.id).delete
                  orderShippingMethod ← OrderShippingMethods.saveNew(orderShipping)
                  shipments ← Shipments.filter(_.orderId === order.id).map(_.orderShippingMethodId).update(Some(orderShippingMethod.id))
                  order ← fullOrder(finder)
                } yield order)
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
}
