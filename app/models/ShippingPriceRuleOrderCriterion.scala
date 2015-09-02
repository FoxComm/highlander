package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._

import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


final case class ShippingPriceRuleOrderCriterion(id:Int = 0, orderCriterionId: Int, shippingPricingRuleId:Int) extends ModelWithIdParameter

object ShippingPriceRuleOrderCriterion

class ShippingPriceRulesOrderCriteria(tag: Tag) extends GenericTable.TableWithId[ShippingPriceRuleOrderCriterion](tag, "shipping_price_rules_order_criteria") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderCriterionId = column[Int]("order_criterion_id")
  def shippingPricingRuleId = column[Int]("shipping_price_rule_id")

  def * = (id, orderCriterionId, shippingPricingRuleId) <> ((ShippingPriceRuleOrderCriterion.apply _).tupled, ShippingPriceRuleOrderCriterion.unapply)
}

object ShippingPriceRulesOrderCriteria extends TableQueryWithId[ShippingPriceRuleOrderCriterion, ShippingPriceRulesOrderCriteria](
  idLens = GenLens[ShippingPriceRuleOrderCriterion](_.id)
)(new ShippingPriceRulesOrderCriteria(_)) {
  def criteriaForPricingRule(id:Int)
                            (implicit ec: ExecutionContext,
                             db: Database): Future[Seq[OrderPriceCriterion]] = {
    // So, this is a hack.  Really, we should be collecting all criteria types.  I'm not sure the best approach there, so I'm going to just return a pricingCriterion.
    // TODO: Solve this using an optimal/good polymorphic approach.
    db.run (
      ( for {
        criteriaMappings ← filter(_.shippingPricingRuleId === id)
        criteria ← OrderPriceCriteria.filter(_.id === criteriaMappings.orderCriterionId)
      } yield (criteria) ).result
    )
  }
}
