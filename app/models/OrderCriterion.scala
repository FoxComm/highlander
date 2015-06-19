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


case class OrderCriterion(id:Int = 0, shippingPricingRuleId:Int, name: String, criterionType: OrderCriterion.CriterionType, target: String) extends ModelWithIdParameter


object OrderCriterion{
  sealed trait CriterionType
  // So, each of the below has it's own table.  I wonder if the right move is to just call this object and centralize all the finding and logic here?
  case object Destination extends CriterionType
  case object Weight extends CriterionType
  case object Price extends CriterionType // before discount
  case object Dimensions extends CriterionType
}

class OrderCriteria(tag: Tag) extends GenericTable.TableWithId[OrderCriteria](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  def * = (id, shippingPricingRuleId, name, criterionType, target) <> ((OrderCriteria.apply _).tupled, OrderCriteria.unapply)
}

object OrderCriteria extends TableQueryWithId[OrderCriteria, OrderCriteria](
  idLens = GenLens[OrderCriteria](_.id)
)(new OrderCriteria(_))