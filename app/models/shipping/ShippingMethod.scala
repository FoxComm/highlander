package models.shipping

import cats.data.Xor
import failures.Failures
import failures.ShippingMethodFailures.ShippingMethodIsNotActive
import models.order.{Order, OrderShippingMethods}
import models.rules.QueryStatement
import monocle.macros.GenLens
import utils.db.ExPostgresDriver.api._
import utils.db._

case class ShippingMethod(id: Int = 0, parentId: Option[Int] = None, adminDisplayName: String,
  storefrontDisplayName: String, shippingCarrierId: Option[Int] = None, price: Int, isActive: Boolean = true,
  conditions: Option[QueryStatement] = None, restrictions: Option[QueryStatement] = None)
  extends FoxModel[ShippingMethod] {

  def mustBeActive: Failures Xor ShippingMethod =
    if(isActive) Xor.right(this) else Xor.left(ShippingMethodIsNotActive(id).single)

}

object ShippingMethod

class ShippingMethods(tag: Tag) extends FoxTable[ShippingMethod](tag, "shipping_methods")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId = column[Option[Int]]("parent_id")
  def adminDisplayName = column[String]("admin_display_name")
  def storefrontDisplayName = column[String]("storefront_display_name")
  def shippingCarrierId = column[Option[Int]]("shipping_carrier_id")
  def price = column[Int]("price")
  def isActive = column[Boolean]("is_active")
  def conditions = column[Option[QueryStatement]]("conditions")
  def restrictions = column[Option[QueryStatement]]("restrictions")

  def * = (id, parentId, adminDisplayName, storefrontDisplayName, shippingCarrierId, price,
    isActive, conditions, restrictions) <> ((ShippingMethod.apply _).tupled, ShippingMethod.unapply)
}

object ShippingMethods extends FoxTableQuery[ShippingMethod, ShippingMethods](
  idLens = GenLens[ShippingMethod](_.id)
)(new ShippingMethods(_)) {

  def findActive: Query[ShippingMethods, ShippingMethod, Seq] = filter(_.isActive === true)

  def findActiveById(id: Int): QuerySeq = findActive.filter(_.id === id)

  def forOrder(order: Order): QuerySeq = for {
    orderShippingMethod ← OrderShippingMethods.filter(_.orderId === order.id)
    shipMethod          ← ShippingMethods.filter(_.id === orderShippingMethod.shippingMethodId)
  } yield shipMethod
}
