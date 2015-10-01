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

final case class OrderShippingMethod(id: Int = 0, orderId: Int = 0, adminDisplayName: String,
  storefrontDisplayName: String, shippingCarrierId: Option[Int] = None, price: Int)
  extends ModelWithIdParameter
  with NewModel
  with Validation[OrderShippingMethod] {

  import Validation._

  def isNew: Boolean = id == 0
  def instance: OrderShippingMethod = { this }

  def validate: ValidatedNel[Failure, OrderShippingMethod] = {
    ( notEmpty(adminDisplayName, "adminDisplayName")
      |@| notEmpty(storefrontDisplayName, "storefrontDisplayName")
    ).map { case _ ⇒ this }
  }
}

object OrderShippingMethod {
  def buildFromShippingMethod(sm: ShippingMethod): OrderShippingMethod =
    OrderShippingMethod(adminDisplayName = sm.adminDisplayName, storefrontDisplayName = sm.storefrontDisplayName,
      shippingCarrierId = sm.shippingCarrierId, price = sm.price)
}

class OrderShippingMethods(tag: Tag) extends TableWithId[OrderShippingMethod](tag, "order_shipping_methods")
   {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def adminDisplayName = column[String]("admin_display_name")
  def storefrontDisplayName = column[String]("storefront_display_name")
  def shippingCarrierId = column[Option[Int]]("shipping_carrier_id")
  def price = column[Int]("price")

  def * = (id, orderId, adminDisplayName, storefrontDisplayName,
    shippingCarrierId, price) <> ((OrderShippingMethod.apply _).tupled, OrderShippingMethod.unapply)

  def order = foreignKey(Orders.tableName, orderId, Orders)(_.id)
}

object OrderShippingMethods extends TableQueryWithId[OrderShippingMethod, OrderShippingMethods](
  idLens = GenLens[OrderShippingMethod](_.id)
)(new OrderShippingMethods(_)) {
  def copyFromShippingMethod(sm: ShippingMethod, order: Order)(implicit ec: ExecutionContext):
  DBIO[OrderShippingMethod] =
    save(OrderShippingMethod.buildFromShippingMethod(sm).copy(orderId = order.id))
}
