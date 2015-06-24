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


case class ShippingPriceRule(id:Int = 0, name: String, ruleType: ShippingPriceRule.RuleType, flatPrice: Int, flatMarkup: Int) extends ModelWithIdParameter

object ShippingPriceRule{
  sealed trait RuleType
  case object Flat extends RuleType
  case object FromCarrier extends RuleType

  implicit val RuleTypeColumn = MappedColumnType.base[RuleType, String]({
    case t => t.toString.toLowerCase
  },
  {
    case "flat" => Flat
    case "fromcarrier" => FromCarrier
    case unknown => throw new IllegalArgumentException(s"cannot map price_type column to type $unknown")

  })
}


class ShippingPriceRules(tag: Tag) extends GenericTable.TableWithId[ShippingPriceRule](tag, "shipping_price_rules") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def ruleType = column[ShippingPriceRule.RuleType]("rule_type")
  def flatPrice = column[Int]("flat_price")
  def flatMarkup = column[Int]("flat_markup") // can be negative.  eg. markdown

  def * = (id, name, ruleType, flatPrice, flatMarkup) <> ((ShippingPriceRule.apply _).tupled, ShippingPriceRule.unapply)
}

object ShippingPriceRules extends TableQueryWithId[ShippingPriceRule, ShippingPriceRules](
  idLens = GenLens[ShippingPriceRule](_.id)
)(new ShippingPriceRules(_)){
  val methodPriceRuleMapping = ShippingMethodsPriceRules

  def shippingPriceRulesForShippingMethod(id: Int)
                                        (implicit ec: ExecutionContext, db: Database): Future[Seq[ShippingPriceRule]] = {
    db.run(
      ( for {
      shippingMethods ← methodPriceRuleMapping.filter(_.shippingMethodId === id)
      shippingPriceRule ← this.filter(_.id === shippingMethods.shippingPriceRuleId)
    } yield (shippingPriceRule) ).result
    )
  }
}