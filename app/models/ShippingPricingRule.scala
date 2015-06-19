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


case class ShippingPricingRule(id:Int = 0, adminDisplayName: String, storefrontDisplayName: String, shippingCarrierId: Int, defaultPrice: Int, isActive: Boolean = true) extends ModelWithIdParameter

object ShippingPricingRule

class ShippingPricingRules(tag: Tag) extends GenericTable.TableWithId[ShippingPricingRule](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def ruleType = column[RuleType]("rule_type")

  def * = (id, adminDisplayName, storefrontDisplayName, shippingCarrierId, defaultPrice, isActive) <> ((ShippingPricingRule.apply _).tupled, ShippingPricingRule.unapply)
}

object ShippingPricingRules extends TableQueryWithId[ShippingPricingRule, ShippingPricingRules](
  idLens = GenLens[ShippingPricingRule](_.id)
)(new ShippingPricingRules(_))