package models.shipping

import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.db._

case class ShippingPriceRule(id:Int = 0, name: String, ruleType: ShippingPriceRule.RuleType, flatPrice: Int, flatMarkup: Int)
  extends FoxModel[ShippingPriceRule]

object ShippingPriceRule{
  sealed trait RuleType
  case object Flat extends RuleType
  case object FromCarrier extends RuleType

  implicit val RuleTypeColumn: JdbcType[RuleType] with BaseTypedType[RuleType] = MappedColumnType.base[RuleType, String]({
    case t ⇒ t.toString.toLowerCase
  },
  {
    case "flat" ⇒ Flat
    case "fromcarrier" ⇒ FromCarrier
    case unknown ⇒ throw new IllegalArgumentException(s"cannot map price_type column to type $unknown")

  })
}


class ShippingPriceRules(tag: Tag) extends FoxTable[ShippingPriceRule](tag, "shipping_price_rules")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def ruleType = column[ShippingPriceRule.RuleType]("rule_type")
  def flatPrice = column[Int]("flat_price")
  def flatMarkup = column[Int]("flat_markup") // can be negative.  eg. markdown

  def * = (id, name, ruleType, flatPrice, flatMarkup) <> ((ShippingPriceRule.apply _).tupled, ShippingPriceRule.unapply)
}

object ShippingPriceRules extends FoxTableQuery[ShippingPriceRule, ShippingPriceRules](new ShippingPriceRules(_))
  with ReturningId[ShippingPriceRule, ShippingPriceRules] {
  val returningLens: Lens[ShippingPriceRule, Int] = lens[ShippingPriceRule].id
  val methodPriceRuleMapping = ShippingMethodsPriceRules

}
