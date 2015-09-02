package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._

import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


final case class ShippingRestriction(id:Int = 0, restrictionType: ShippingRestriction.RestrictionType, name: String, displayAnyway: Boolean) extends ModelWithIdParameter

object ShippingRestriction{
  sealed trait RestrictionType
  case object ShipTo extends RestrictionType //Use OrderCriterion
  case object ItemAttribute extends RestrictionType //Use SkuCriterion

  implicit val DestinationColumnType = MappedColumnType.base[RestrictionType, String]({
    case t=> t.toString.toLowerCase
  },
  {
    case "shipto" => ShipTo
    case "itemattribute" => ItemAttribute
    case unknown => throw new IllegalArgumentException(s"cannot map destination_type column to type $unknown")
  })
}

class ShippingRestrictions(tag: Tag) extends GenericTable.TableWithId[ShippingRestriction](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def restrictionType = column[ShippingRestriction.RestrictionType]("restriction_type")
  def name = column[String]("name")
  def displayAnyway = column[Boolean]("display_anyway") //Do we still list this as an option, even if it won't be allowed.

  def * = (id, restrictionType, name, displayAnyway) <> ((ShippingRestriction.apply _).tupled, ShippingRestriction.unapply)
}

object ShippingRestrictions extends TableQueryWithId[ShippingRestriction, ShippingRestrictions](
  idLens = GenLens[ShippingRestriction](_.id)
)(new ShippingRestrictions(_))
