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


final case class OrderDestinationCriterion(id:Int = 0, destinationType: OrderDestinationCriterion.DestinationType, destination: String, exclude: Boolean) extends ModelWithIdParameter

object OrderDestinationCriterion{
  sealed trait DestinationType
  case object Country extends DestinationType
  case object State extends DestinationType
  case object City extends DestinationType
  case object ShippingZone extends DestinationType // This will likely be carrier-specific

  implicit val DestinationColumnType: JdbcType[DestinationType] with BaseTypedType[DestinationType] = MappedColumnType.base[DestinationType, String]({
    case t=> t.toString.toLowerCase
  },
  {
    case "country" => Country
    case "state" => State
    case "city" => City
    case "shippingzone" => ShippingZone
    case unknown => throw new IllegalArgumentException(s"cannot map destination_type column to type $unknown")
  })
}

class OrderDestinationCriteria(tag: Tag) extends GenericTable.TableWithId[OrderDestinationCriterion](tag, "shipping_methods")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def destinationType = column[OrderDestinationCriterion.DestinationType]("destination_type")
  def destination = column[String]("destination") //Great candidation for JSON schema
  def exclude = column[Boolean]("exclude") // Is this an inclusion or exclusion rule?

  def * = (id, destinationType, destination, exclude) <> ((OrderDestinationCriterion.apply _).tupled, OrderDestinationCriterion.unapply)
}

object OrderDestinationCriteria extends TableQueryWithId[OrderDestinationCriterion, OrderDestinationCriteria](
  idLens = GenLens[OrderDestinationCriterion](_.id)
)(new OrderDestinationCriteria(_))
