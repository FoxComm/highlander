package models.order

import models.shipping.{ShippingMethod, ShippingMethods}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class OrderShippingMethod(id: Int = 0, orderId: Int, shippingMethodId: Int, price: Int)
    extends FoxModel[OrderShippingMethod]

object OrderShippingMethod {
  def build(order: Order, method: ShippingMethod): OrderShippingMethod =
    OrderShippingMethod(orderId = order.id, shippingMethodId = method.id, price = method.price)
}

class OrderShippingMethods(tag: Tag)
    extends FoxTable[OrderShippingMethod](tag, "order_shipping_methods") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId          = column[Int]("order_id")
  def shippingMethodId = column[Int]("shipping_method_id")
  def price            = column[Int]("price")

  def * =
    (id, orderId, shippingMethodId, price) <> ((OrderShippingMethod.apply _).tupled, OrderShippingMethod.unapply)

  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def shippingMethod =
    foreignKey(ShippingMethods.tableName, shippingMethodId, ShippingMethods)(_.id)
}

object OrderShippingMethods
    extends FoxTableQuery[OrderShippingMethod, OrderShippingMethods](new OrderShippingMethods(_))
    with ReturningId[OrderShippingMethod, OrderShippingMethods] {

  val returningLens: Lens[OrderShippingMethod, Int] = lens[OrderShippingMethod].id

  def findByOrderId(orderId: Int): QuerySeq = filter(_.orderId === orderId)
}
