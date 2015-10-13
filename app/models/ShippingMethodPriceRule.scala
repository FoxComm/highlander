package models

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}


final case class ShippingMethodPriceRule(id:Int = 0, shippingMethodId: Int, shippingPriceRuleId:Int, ruleRank: Int) extends ModelWithIdParameter

object ShippingMethodPriceRule

class ShippingMethodsPriceRules(tag: Tag) extends GenericTable.TableWithId[ShippingMethodPriceRule](tag, "shipping_methods_price_rules")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def shippingMethodId = column[Int]("shipping_method_id")
  def shippingPriceRuleId = column[Int]("shipping_price_rule_id")
  def rule_rank = column[Int]("rule_rank")

  def * = (id, shippingMethodId, shippingPriceRuleId, rule_rank) <> ((ShippingMethodPriceRule.apply _).tupled, ShippingMethodPriceRule.unapply)
}

object ShippingMethodsPriceRules extends TableQueryWithId[ShippingMethodPriceRule, ShippingMethodsPriceRules](
  idLens = GenLens[ShippingMethodPriceRule](_.id)
)(new ShippingMethodsPriceRules(_))
