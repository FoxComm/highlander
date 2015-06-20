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


case class OrderPriceCriterion(id:Int = 0, priceType: OrderPriceCriterion.PriceType, greaterThan: Int, lessThan: Int, exactMatch: Int, currency: String, exclude: Boolean) extends ModelWithIdParameter

object OrderPriceCriterion{
  sealed trait PriceType
  case object SubTotal extends PriceType
  case object GrandTotal extends PriceType
  case object GrandTotalLessTax extends PriceType
  case object GrandTotalLessShipping extends PriceType

  implicit val PriceTypeColumn = MappedColumnType.base[PriceType, String]({
    case t => t.toString.toLowerCase
  },
  {
    case "subtotal" => SubTotal
    case "grandtotal" => GrandTotal
    case "grandtotallesstax" => GrandTotalLessTax
    case "grandtotallessshipping" => GrandTotalLessShipping
    case unknown => throw new IllegalArgumentException(s"cannot map price_type column to type $unknown")

  })
}

class OrderPriceCriteria(tag: Tag) extends GenericTable.TableWithId[OrderPriceCriterion](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def priceType = column[OrderPriceCriterion.PriceType]("price_type")
  def greaterThan = column[Int]("greater_than")
  def lessThan = column[Int]("less_than")
  def exactMatch = column[Int]("exact_match") // Doesn't seem likely that anyone would use this.  But the pattern applies..
  def currency = column[String]("currency") // USD, GBP, etc
  def exclude = column[Boolean]("exclude") // Is this an inclusion or exclusion rule?

  def * = (id, priceType, greaterThan, lessThan, exactMatch, currency, exclude) <> ((OrderPriceCriterion.apply _).tupled, OrderPriceCriterion.unapply)
}

object OrderPriceCriteria extends TableQueryWithId[OrderPriceCriterion, OrderPriceCriteria](
  idLens = GenLens[OrderPriceCriterion](_.id)
)(new OrderPriceCriteria(_))