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


case class ShippingRuleCriterion(id:Int = 0, shippingPricingRuleId:Int, name: String, criterionType: ShippingRuleCriterion.CriterionType, target: String) extends ModelWithIdParameter

object ShippingRuleCriterion{
  sealed trait CriterionType
  case object Destination extends CriterionType
  case object Weight extends CriterionType
  case object SubTotal extends CriterionType // before discount
  case object GrandTotal extends CriterionType // after discounts and tax
  case object Dimensions extends CriterionType
  case object PriceAttribute extends CriterionType
}

// I can't decide if I want to make these fields generalizable or make a different table for each CriterionType.
// Going to do it as one table for now
class ShippingRuleCriteria(tag: Tag) extends GenericTable.TableWithId[ShippingRuleCriteria](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def shippingPricingRuleId = column[Int]("shipping_pricing_rule_id")
  def name = column[String]("name")
  def criterionType = column[ShippingRuleCriterion.CriterionType]("criterion_type")
  def greaterThanInt = column[Int]("greater_than_int")
  def lessThanInt = column[Int]("less_than_int")
  def equalsInt = column[Int]("equals_int")
  def unitOfMeasure = column[String]("unit_of_measure")
  def includedInString = column[String]("included_in_string") // Destinations, mostly


  def * = (id, shippingPricingRuleId, name, criterionType, target) <> ((ShippingRuleCriteria.apply _).tupled, ShippingRuleCriteria.unapply)
}

object ShippingRuleCriteria extends TableQueryWithId[ShippingRuleCriteria, ShippingRuleCriteria](
  idLens = GenLens[ShippingRuleCriteria](_.id)
)(new ShippingRuleCriteria(_))