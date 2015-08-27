package models

import org.json4s.JValue
import org.json4s.jackson.JsonMethods._
import utils.{GenericTable, TableQueryWithId, ModelWithIdParameter, RichTable}
import utils.ExPostgresDriver.api._
import monocle.macros.GenLens

final case class ShippingMethod(id:Int = 0, adminDisplayName: String, storefrontDisplayName: String,
  shippingCarrierId: Option[Int] = None, defaultPrice: Int, isActive: Boolean = true,
  conditions: JValue = parse("""{ "error": "message" }"""))
  extends ModelWithIdParameter

object ShippingMethod

class ShippingMethods(tag: Tag) extends GenericTable.TableWithId[ShippingMethod](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminDisplayName = column[String]("admin_display_name")
  def storefrontDisplayName = column[String]("storefront_display_name")
  def shippingCarrierId = column[Option[Int]]("shipping_carrier_id")
  def defaultPrice = column[Int]("default_price") // this is only used if the pricing rules return an invalid response
  def isActive = column[Boolean]("is_active")
  def conditions = column[JValue]("conditions")

  def * = (id, adminDisplayName, storefrontDisplayName, shippingCarrierId, defaultPrice,
    isActive, conditions) <> ((ShippingMethod.apply _).tupled, ShippingMethod.unapply)
}

object ShippingMethods extends TableQueryWithId[ShippingMethod, ShippingMethods](
  idLens = GenLens[ShippingMethod](_.id)
)(new ShippingMethods(_))
