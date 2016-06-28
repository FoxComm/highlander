package models.order

import models.shipping.{ShippingMethod, ShippingMethods}
import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class OrderShippingMethod(id: Int = 0, orderRef: String, shippingMethodId: Int, price: Int)
    extends FoxModel[OrderShippingMethod]

object OrderShippingMethod {
  def build(order: Order, method: ShippingMethod): OrderShippingMethod =
    OrderShippingMethod(orderRef = order.refNum,
                        shippingMethodId = method.id,
                        price = method.price)
}

class OrderShippingMethods(tag: Tag)
    extends FoxTable[OrderShippingMethod](tag, "order_shipping_methods") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderRef         = column[String]("order_ref")
  def shippingMethodId = column[Int]("shipping_method_id")
  def price            = column[Int]("price")

  def * =
    (id, orderRef, shippingMethodId, price) <> ((OrderShippingMethod.apply _).tupled, OrderShippingMethod.unapply)

  def order = foreignKey(Orders.tableName, orderRef, Orders)(_.referenceNumber)
  def shippingMethod =
    foreignKey(ShippingMethods.tableName, shippingMethodId, ShippingMethods)(_.id)
}

object OrderShippingMethods
    extends FoxTableQuery[OrderShippingMethod, OrderShippingMethods](new OrderShippingMethods(_))
    with ReturningId[OrderShippingMethod, OrderShippingMethods] {

  val returningLens: Lens[OrderShippingMethod, Int] = lens[OrderShippingMethod].id

  def findByOrderRef(orderRef: String): QuerySeq = filter(_.orderRef === orderRef)
}
