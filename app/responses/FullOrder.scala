package responses

import models._

import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import scala.concurrent.{ExecutionContext, Future}

object FullOrder {
  type Response = Future[Option[Root]]

  case class Totals(subTotal: Int, taxes: Int, adjustments: Int, total: Int)
  case class Root(id: Int, referenceNumber: Option[String],
                  orderStatus: Order.Status,
                  shippingStatus: Order.Status,
                  paymentStatus: Order.Status,
                  lineItems: Seq[DisplayLineItem],
                  adjustments: Seq[Adjustment],
                  fraudScore: Int,
                  totals: Totals,
                  customer: Option[Customer],
                  shippingMethod: Option[ShippingMethod],
                  shippingAddress: Option[Address],
                  paymentMethods: Seq[PaymentMethod] = Seq.empty)

  case class DisplayLineItem(imagePath: String = "http://lorempixel.com/75/75/fashion" ,
                              name: String = "donkey product",
                              skuId: Int,
                              price: Int = 33,
                              qty: Int = 1,
                              status: OrderLineItem.Status )

  def build(order: Order, lineItems: Seq[OrderLineItem] = Seq.empty, adjustments: Seq[Adjustment] = Seq.empty, shippingMethod: Option[ShippingMethod] = None, customer: Option[Customer] = None, shippingAddress: Option[Address] = None): Root = {
    val rand = scala.util.Random
    Root(id = order.id,
      referenceNumber = order.referenceNumber,
      orderStatus = order.status,
      shippingStatus = order.status,
      paymentStatus = order.status,
      lineItems = lineItems.map{oli => DisplayLineItem(skuId = oli.skuId, status = oli.status)},
      adjustments = adjustments,
      fraudScore = rand.nextInt(100),
      customer = customer,
      shippingAddress = shippingAddress,
      totals = Totals(subTotal = 333, taxes = 10, adjustments = 0, total = 510), shippingMethod = shippingMethod)
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
      //shipMethodMapping <- OrdersShippingMethods.filter(_.orderId === order.id)
      shipment <- Shipments.filter(_.orderId === order.id)
      customer <- Customers._findById(order.customerId)
      shipMethod <- ShippingMethods.filter(_.id === shipment.shippingMethodId)
      address <- Addresses.filter(_.id === shipment.shippingAddressId)
      //payment <- PaymentMethods
    } yield (order, lineItems, shipMethod, customer, address)

    db.run(queries.result).map { results =>
      results.headOption.map { case (order, _, shippingMethod, customer, address) =>
        build(order = order, lineItems = results.map { case (_, items, _, _, _) => items }, shippingMethod = Some(shippingMethod), customer = Some(customer), shippingAddress = Some(address))
      }
    }
  }
}
