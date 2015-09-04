package models

import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._

import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


final case class OrderDimensionCriterion(id:Int = 0, dimensionType: OrderDimensionCriterion.DimensionType, greaterThan: Option[Int], lessThan: Option[Int], exactMatch: Option[Int], unitOfMeasure: String, exclude: Boolean) extends ModelWithIdParameter

object OrderDimensionCriterion{
  sealed trait DimensionType
  case object LinearDimensions extends DimensionType //Length + Width + Height
  case object CubicDensity extends DimensionType //Like above, but fancier
  case object LengthOnly extends DimensionType
  case object HeightOnly extends DimensionType
  case object WidthOnly extends DimensionType

  implicit val DimensionTypeColumn: JdbcType[DimensionType] with BaseTypedType[DimensionType] = MappedColumnType.base[DimensionType, String]({
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

class OrderDimensionCriteria(tag: Tag) extends GenericTable.TableWithId[OrderDimensionCriterion](tag, "shipping_methods")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def dimensionType = column[OrderDimensionCriterion.DimensionType]("dimension_type")
  def greaterThan = column[Option[Int]]("greater_than")
  def lessThan = column[Option[Int]]("less_than")
  def exactMatch = column[Option[Int]]("exact_match") // Doesn't seem likely that anyone would use this.  But the pattern applies..
  def unitOfMeasure = column[String]("unit_of_measure") // Inches, CM, etc
  def exclude = column[Boolean]("exclude") // Is this an inclusion or exclusion rule?

  def * = (id, dimensionType, greaterThan, lessThan, exactMatch, unitOfMeasure, exclude) <> ((OrderDimensionCriterion.apply _).tupled, OrderDimensionCriterion.unapply)
}

object OrderDimensionCriteria extends TableQueryWithId[OrderDimensionCriterion, OrderDimensionCriteria](
  idLens = GenLens[OrderDimensionCriterion](_.id)
)(new OrderDimensionCriteria(_))
