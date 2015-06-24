package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object FullOrder {
  type Response = Future[Option[Root]]

  case class Totals(subTotal: Int, taxes: Int, adjustments: Int, total: Int)
  case class Root(id: Int, referenceNumber: Option[String], orderStatus: Order.Status, lineItems: Seq[OrderLineItem], adjustments: Seq[Adjustment], totals: Totals, shippingMethod: Option[ShippingMethod])

  def build(order: Order, lineItems: Seq[OrderLineItem] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty, shippingMethod: Option[ShippingMethod] = None): Root = {
    Root(id = order.id, referenceNumber = order.referenceNumber, orderStatus = order.status, lineItems = lineItems, adjustments = adjustments, totals =
      Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510), shippingMethod = shippingMethod)
  }

  def findById(id: Int)
              (implicit ec: ExecutionContext, db: Database): Response = {
    this.findOrder(Orders._findById(id))
  }

  def findByCustomer(customer: Customer)
                    (implicit ec: ExecutionContext, db: Database): Response = {
    this.findOrder(Orders._findByCustomer(customer))
  }

  def findCartByCustomer(customer: Customer)
                        (implicit ec: ExecutionContext, db: Database): Response = {
    this.findOrder(Orders._findActiveOrderByCustomer(customer))
  }

  def fromOrder(order: Order)
              (implicit ec: ExecutionContext,
               db: Database): Response = {

    val queries = for {
      lineItems <- OrderLineItems._findByOrderId(order.id)
    } yield (lineItems)

    db.run(queries.result).map { lineItems => Some(build(order, lineItems)) }
  }

  private [this] def findOrder(finder: Query[Orders, Order, Seq])
                             (implicit ec: ExecutionContext,
                              db: Database): Response = {
    val queries = for {
      order <- finder
      lineItems <- OrderLineItems._findByOrderId(order.id)
      shipMethodMapping <- OrdersShippingMethods.filter(_.orderId === order.id)
      shipMethod <- ShippingMethods.filter(_.id === shipMethodMapping.shippingMethodId)
    } yield (order, lineItems, shipMethod)

    db.run(queries.result).map { results =>
      results.headOption.map { case (order, _, shippingMethod) =>
        build(order = order, lineItems = results.map { case (_, items, _) => items }, shippingMethod = Some(shippingMethod))
      }
    }
  }
}
