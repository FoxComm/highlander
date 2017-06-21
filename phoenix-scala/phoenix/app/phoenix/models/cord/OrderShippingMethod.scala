package phoenix.models.cord

import phoenix.models.shipping.{ShippingMethod, ShippingMethods}
import shapeless._
import slick.jdbc.PostgresProfile.api._
import core.db._

case class OrderShippingMethod(id: Int = 0, cordRef: String, shippingMethodId: Int, price: Long)
    extends FoxModel[OrderShippingMethod]

object OrderShippingMethod {
  def build(cordRef: String, method: ShippingMethod): OrderShippingMethod =
    OrderShippingMethod(cordRef = cordRef, shippingMethodId = method.id, price = method.price)
}

class OrderShippingMethods(tag: Tag) extends FoxTable[OrderShippingMethod](tag, "order_shipping_methods") {
  def id               = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef          = column[String]("cord_ref")
  def shippingMethodId = column[Int]("shipping_method_id")
  def price            = column[Long]("price")

  def * =
    (id, cordRef, shippingMethodId, price) <> ((OrderShippingMethod.apply _).tupled, OrderShippingMethod.unapply)

  def order = foreignKey(Carts.tableName, cordRef, Carts)(_.referenceNumber)
  def shippingMethod =
    foreignKey(ShippingMethods.tableName, shippingMethodId, ShippingMethods)(_.id)
}

object OrderShippingMethods
    extends FoxTableQuery[OrderShippingMethod, OrderShippingMethods](new OrderShippingMethods(_))
    with ReturningId[OrderShippingMethod, OrderShippingMethods] {

  val returningLens: Lens[OrderShippingMethod, Int] = lens[OrderShippingMethod].id

  def findByOrderRef(cordRef: String): QuerySeq = filter(_.cordRef === cordRef)
}
