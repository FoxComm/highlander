package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class OrderShippingMethod(orderId: Int = 0, shippingMethodId: Int, shippingPrice: Option[Int] = None) extends ModelWithIdParameter

object OrderShippingMethod

class OrdersShippingMethods(tag: Tag) extends GenericTable.TableWithId[OrderShippingMethod](tag, "orders_shipping_methods") with RichTable {
  def id = orderId
  def orderId = column[Int]("order_id", O.PrimaryKey) // foreign key to order(id)
  def shippingMethodId = column[Int]("shipping_method_id")
  def shippingPrice = column[Option[Int]]("shipping_price") //gets filled in upon checkout

  def * = (orderId, shippingMethodId, shippingPrice) <> ((OrderShippingMethod.apply _).tupled, OrderShippingMethod.unapply)
}

object OrdersShippingMethods extends TableQueryWithId[OrderShippingMethod, OrdersShippingMethods](
  idLens = GenLens[OrderShippingMethod](_.orderId)
)(new OrdersShippingMethods(_))