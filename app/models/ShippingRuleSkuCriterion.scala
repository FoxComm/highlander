package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._

import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


final case class ShippingRuleSkuCriterion(id:Int = 0, shippingPricingRuleId:Int, skuCriterionId: Int) extends ModelWithIdParameter

object ShippingRuleSkuCriterion

class ShippingRuleSkuCriteria(tag: Tag) extends GenericTable.TableWithId[ShippingRuleSkuCriterion](tag, "shipping_methods")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def skuCriterionId = column[Int]("sku_criterion_id")
  def shippingPricingRuleId = column[Int]("shipping_price_rule_id")

  def * = (id, skuCriterionId, shippingPricingRuleId) <> ((ShippingRuleSkuCriterion.apply _).tupled, ShippingRuleSkuCriterion.unapply)
}

object ShippingRuleSkuCriteria extends TableQueryWithId[ShippingRuleSkuCriterion, ShippingRuleSkuCriteria](
  idLens = GenLens[ShippingRuleSkuCriterion](_.id)
)(new ShippingRuleSkuCriteria(_))
