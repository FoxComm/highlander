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


case class ShippingPriceRuleOrderCriterion(id:Int = 0, orderCriterionId: Int, shippingPricingRuleId:Int) extends ModelWithIdParameter

object ShippingPriceRuleOrderCriterion

class ShippingPriceRulesOrderCriteria(tag: Tag) extends GenericTable.TableWithId[ShippingPriceRuleOrderCriterion](tag, "shipping_price_rules_order_criteria") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderCriterionId = column[Int]("order_criterion_id")
  def shippingPricingRuleId = column[Int]("shipping_price_rule_id")

  def * = (id, orderCriterionId, shippingPricingRuleId) <> ((ShippingPriceRuleOrderCriterion.apply _).tupled, ShippingPriceRuleOrderCriterion.unapply)
}

object ShippingPriceRulesOrderCriteria extends TableQueryWithId[ShippingPriceRuleOrderCriterion, ShippingPriceRulesOrderCriteria](
  idLens = GenLens[ShippingPriceRuleOrderCriterion](_.id)
)(new ShippingPriceRulesOrderCriteria(_))