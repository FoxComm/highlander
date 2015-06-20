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


case class ShippingPricingRule(id:Int = 0, name: String, ruleType: ShippingPricingRule.RuleType, flatPrice: Int, flatMarkup: Int) extends ModelWithIdParameter

object ShippingPricingRule{
  sealed trait RuleType
  case object Flat extends RuleType
  case object FromCarrier extends RuleType

  implicit val RuleTypeColumn = MappedColumnType.base[RuleType, String]({
    case t => t.toString.toLowerCase
  },
  {
    case "flat" => Flat
    case "fromcarrier" => FromCarrier
    case unknown => throw new IllegalArgumentException(s"cannot map price_type column to type $unknown")

  })
}


class ShippingPricingRules(tag: Tag) extends GenericTable.TableWithId[ShippingPricingRule](tag, "shipping_pricing_rules") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def ruleType = column[ShippingPricingRule.RuleType]("rule_type")
  def flatPrice = column[Int]("flat_price")
  def flatMarkup = column[Int]("flat_markup") // can be negative.  eg. markdown

  def * = (id, name, ruleType, flatPrice, flatMarkup) <> ((ShippingPricingRule.apply _).tupled, ShippingPricingRule.unapply)
}

object ShippingPricingRules extends TableQueryWithId[ShippingPricingRule, ShippingPricingRules](
  idLens = GenLens[ShippingPricingRule](_.id)
)(new ShippingPricingRules(_))