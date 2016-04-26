package models.shipping

import shapeless._
import slick.driver.PostgresDriver.api._
import utils.db._

case class ShippingMethodPriceRule(id:Int = 0, shippingMethodId: Int, shippingPriceRuleId:Int, ruleRank: Int)
  extends FoxModel[ShippingMethodPriceRule]

object ShippingMethodPriceRule

class ShippingMethodsPriceRules(tag: Tag) extends FoxTable[ShippingMethodPriceRule](tag, "shipping_methods_price_rules")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def shippingMethodId = column[Int]("shipping_method_id")
  def shippingPriceRuleId = column[Int]("shipping_price_rule_id")
  def ruleRank = column[Int]("rule_rank")

  def * = (id, shippingMethodId, shippingPriceRuleId, ruleRank) <> ((ShippingMethodPriceRule.apply _).tupled, ShippingMethodPriceRule.unapply)
}

object ShippingMethodsPriceRules extends FoxTableQuery[ShippingMethodPriceRule, ShippingMethodsPriceRules](
  idLens = lens[ShippingMethodPriceRule].id
)(new ShippingMethodsPriceRules(_))
