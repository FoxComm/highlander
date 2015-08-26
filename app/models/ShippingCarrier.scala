package models

import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}


final case class ShippingCarrier(id:Int = 0,
                           name:String,
                           accountNumber:Option[String],
                           regionsServed:String = "US") extends ModelWithIdParameter
object ShippingCarrier

class ShippingCarriers(tag: Tag) extends GenericTable.TableWithId[ShippingCarrier](tag, "shipping_methods") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def accountNumber = column[Option[String]]("account_number")
  def regionsServed = column[String]("regions_served") // Should we enforce this?

  def * = (id, name, accountNumber, regionsServed) <> ((ShippingCarrier.apply _).tupled, ShippingCarrier.unapply)
}

object ShippingCarriers extends TableQueryWithId[ShippingCarrier, ShippingCarriers](
  idLens = GenLens[ShippingCarrier](_.id)
)(new ShippingCarriers(_))
