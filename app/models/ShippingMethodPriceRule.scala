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


case class ShippingMethodPriceRule(id:Int = 0, shippingMethodId: Int, shippingPriceRuleId:Int, ruleRank: Int) extends ModelWithIdParameter

object ShippingMethodPriceRule

class ShippingMethodsPriceRules(tag: Tag) extends GenericTable.TableWithId[ShippingMethodPriceRule](tag, "shipping_methods_pricing_rules") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def shippingMethodId = column[Int]("shipping_method_id")
  def shippingPriceRuleId = column[Int]("shipping_price_rule_id")
  def rule_rank = column[Int]("rule_rank")

  def * = (id, shippingMethodId, shippingPriceRuleId, rule_rank) <> ((ShippingMethodPriceRule.apply _).tupled, ShippingMethodPriceRule.unapply)
}

object ShippingMethodsPriceRules extends TableQueryWithId[ShippingMethodPriceRule, ShippingMethodsPriceRules](
  idLens = GenLens[ShippingMethodPriceRule](_.id)
)(new ShippingMethodsPriceRules(_))