package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, TableQueryWithId}

final case class OrderShippingMethod(id: Int = 0, orderId: Int, shippingMethodId: Int)
  extends ModelWithIdParameter[OrderShippingMethod]

class OrderShippingMethods(tag: Tag) extends TableWithId[OrderShippingMethod](tag, "order_shipping_methods") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def shippingMethodId = column[Int]("shipping_method_id")

  def * = (id, orderId, shippingMethodId) <> ((OrderShippingMethod.apply _).tupled, OrderShippingMethod.unapply)

  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
  def shippingMethod = foreignKey(ShippingMethods.tableName, shippingMethodId, ShippingMethods)(_.id)
}

object OrderShippingMethods extends TableQueryWithId[OrderShippingMethod, OrderShippingMethods](
  idLens = GenLens[OrderShippingMethod](_.id)
)(new OrderShippingMethods(_)) {
  def findByOrderId(orderId: Int)(implicit db: Database): QuerySeq = filter(_.orderId === orderId)
}
