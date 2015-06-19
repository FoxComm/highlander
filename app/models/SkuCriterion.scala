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


case class SkuCriterion(id:Int = 0, name:String) extends ModelWithIdParameter


object SkuCriterion{
  sealed trait CriterionType
  case object Taxonomy extends CriterionType
  case object Collection extends CriterionType
  case object Price extends CriterionType // before discount
  case object Archetype extends CriterionType
  case object Attribute extends CriterionType
}

class SkuCriteria(tag: Tag) extends GenericTable.TableWithId[SkuCriteria](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  def * = (id, name) <> ((SkuCriteria.apply _).tupled, SkuCriteria.unapply)
}

object SkuCriteria extends TableQueryWithId[SkuCriteria, SkuCriteria](
  idLens = GenLens[SkuCriteria](_.id)
)(new SkuCriteria(_))