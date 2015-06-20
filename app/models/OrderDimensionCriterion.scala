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


case class OrderDimensionCriterion(id:Int = 0, priceType: OrderDimensionCriterion.DimensionType, greaterThan: Int, lessThan: Int, exactMatch: Int, unitOfMeasure: String, exclude: Boolean) extends ModelWithIdParameter

object OrderDimensionCriterion{
  sealed trait DimensionType
  case object LinearDimensions extends DimensionType //Length + Width + Height
  case object CubicDensity extends DimensionType //Like above, but fancier
  case object LengthOnly extends DimensionType
  case object HeightOnly extends DimensionType
  case object WidthOnly extends DimensionType

  implicit val DimensionTypeColumn = MappedColumnType.base[DimensionType, String]({
    case t => t.toString.toLowerCase
  },
  {
    case "lineardimensions" => LinearDimensions
    case "cubicdensity" => CubicDensity
    case "lengthonly" => LengthOnly
    case "heightonly" => HeightOnly
    case "widthonly" => WidthOnly
    case unknown => throw new IllegalArgumentException(s"cannot map dimension_type column to type $unknown")
  })
}

class OrderDimensionCriteria(tag: Tag) extends GenericTable.TableWithId[OrderDimensionCriterion](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def priceType = column[OrderDimensionCriterion.DimensionType]("price_type")
  def greaterThan = column[Int]("greater_than")
  def lessThan = column[Int]("less_than")
  def exactMatch = column[Int]("exact_match") // Doesn't seem likely that anyone would use this.  But the pattern applies..
  def unitOfMeasure = column[String]("unit_of_measure") // Inches, CM, etc
  def exclude = column[Boolean]("exclude") // Is this an inclusion or exclusion rule?

  def * = (id, priceType, greaterThan, lessThan, exactMatch, unitOfMeasure, exclude) <> ((OrderDimensionCriterion.apply _).tupled, OrderDimensionCriterion.unapply)
}

object OrderDimensionCriteria extends TableQueryWithId[OrderDimensionCriterion, OrderDimensionCriteria](
  idLens = GenLens[OrderDimensionCriterion](_.id)
)(new OrderDimensionCriteria(_))