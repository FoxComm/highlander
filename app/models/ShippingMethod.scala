package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


final case class ShippingMethod(id:Int = 0, adminDisplayName: String, storefrontDisplayName: String, shippingCarrierId: Option[Int] = None, defaultPrice: Int, isActive: Boolean = true) extends ModelWithIdParameter

object ShippingMethod

class ShippingMethods(tag: Tag) extends GenericTable.TableWithId[ShippingMethod](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminDisplayName = column[String]("admin_display_name")
  def storefrontDisplayName = column[String]("storefront_display_name")
  def shippingCarrierId = column[Option[Int]]("shipping_carrier_id")
  def defaultPrice = column[Int]("default_price") // this is only used if the pricing rules return an invalid response
  def isActive = column[Boolean]("is_active")

  def * = (id, adminDisplayName, storefrontDisplayName, shippingCarrierId, defaultPrice, isActive) <> ((ShippingMethod.apply _).tupled, ShippingMethod.unapply)
}

object ShippingMethods extends TableQueryWithId[ShippingMethod, ShippingMethods](
  idLens = GenLens[ShippingMethod](_.id)
)(new ShippingMethods(_))
