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


case class OrderDestinationCriterion(id:Int = 0, destinationType: OrderDestinationCriterion.DestinationType, destination: String, exclude: Boolean) extends ModelWithIdParameter

object OrderDestinationCriterion{
  sealed trait DestinationType
  case object Country extends DestinationType
  case object State extends DestinationType
  case object City extends DestinationType
  case object ShippingZone extends DestinationType // This will likely be carrier-specific
}

class OrderDestinationCriteria(tag: Tag) extends GenericTable.TableWithId[OrderDestinationCriteria](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def destinationType = column[OrderDestinationCriterion.DestinationType]("criterion_type")
  def destination = column[String]("destination") //Great candidation for JSON schema
  def exclude = column[Boolean]("exclude") // Is this an inclusion or exclusion rule?

  def * = (id, destinationType, destination, exclude) <> ((OrderDestinationCriteria.apply _).tupled, OrderDestinationCriteria.unapply)
}

object OrderDestinationCriteria extends TableQueryWithId[OrderDestinationCriteria, OrderDestinationCriteria](
  idLens = GenLens[OrderDestinationCriteria](_.id)
)(new OrderDestinationCriteria(_))