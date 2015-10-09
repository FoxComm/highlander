package models

import scala.concurrent.ExecutionContext

import cats.data.ValidatedNel
import cats.implicits._
import monocle.macros.GenLens
import services.Failure
import slick.driver.PostgresDriver.api._
import utils.GenericTable.TableWithId
import utils.{Validation, NewModel, ModelWithIdParameter, TableQueryWithId}
import utils.Litterbox._

final case class OrderShippingMethod(id: Int = 0, orderId: Int, shippingMethodId: Int)
  extends ModelWithIdParameter

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

  def copyFromShippingMethod(sm: ShippingMethod, order: Order)(implicit ec: ExecutionContext):
  DBIO[OrderShippingMethod] =
    save(OrderShippingMethod(shippingMethodId = sm.id, orderId = order.id))
}
