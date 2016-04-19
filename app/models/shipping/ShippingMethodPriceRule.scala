package models.shipping

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

case class ShippingMethodPriceRule(id:Int = 0, shippingMethodId: Int, shippingPriceRuleId:Int, ruleRank: Int)
  extends ModelWithIdParameter[ShippingMethodPriceRule]

object ShippingMethodPriceRule

class ShippingMethodsPriceRules(tag: Tag) extends GenericTable.TableWithId[ShippingMethodPriceRule](tag, "shipping_methods_price_rules")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def shippingMethodId = column[Int]("shipping_method_id")
  def shippingPriceRuleId = column[Int]("shipping_price_rule_id")
  def ruleRank = column[Int]("rule_rank")

  def * = (id, shippingMethodId, shippingPriceRuleId, ruleRank) <> ((ShippingMethodPriceRule.apply _).tupled, ShippingMethodPriceRule.unapply)
}

object ShippingMethodsPriceRules extends TableQueryWithId[ShippingMethodPriceRule, ShippingMethodsPriceRules](
  idLens = GenLens[ShippingMethodPriceRule](_.id)
)(new ShippingMethodsPriceRules(_))
