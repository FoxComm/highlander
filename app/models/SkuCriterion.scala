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


final case class SkuCriterion(id:Int = 0, name:String) extends ModelWithIdParameter


object SkuCriterion{
  sealed trait CriterionType
  case object Taxonomy extends CriterionType
  case object Collection extends CriterionType
  case object Price extends CriterionType // before discount
  case object Archetype extends CriterionType
  case object Attribute extends CriterionType

  implicit val CriterionTypeColumnType = MappedColumnType.base[CriterionType, String]({
    case t => t.toString.toLowerCase
  },
  {
    case "taxonomy" => Taxonomy
    case "collection" => Collection
    case "price" => Price
    case "archetype" => Archetype
    case "attribute" => Attribute
  })
}

class SkuCriteria(tag: Tag) extends GenericTable.TableWithId[SkuCriterion](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")

  def * = (id, name) <> ((SkuCriterion.apply _).tupled, SkuCriterion.unapply)
}

object SkuCriteria extends TableQueryWithId[SkuCriterion, SkuCriteria](
  idLens = GenLens[SkuCriterion](_.id)
)(new SkuCriteria(_))
