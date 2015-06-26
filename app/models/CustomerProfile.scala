package models

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import payloads.CreateCustomerPayload
import services.Failure
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils.{GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import org.scalactic._


case class CustomerProfile(customerId: Int, phoneNumber: Option[String] = None, location: Option[String] = None, modality: Option[String] = None) extends ModelWithIdParameter {
  def role = "Customer"
}

class CustomerProfiles(tag: Tag) extends GenericTable.TableWithId[CustomerProfile](tag, "customer_profiles") with RichTable {
  def id = customerId
  def customerId = column[Int]("customer_id", O.PrimaryKey, O.AutoInc)
  def phoneNumber = column[Option[String]]("phone_number")
  def location = column[Option[String]]("location")
  def modality = column[Option[String]]("modality")

  def * = (customerId, phoneNumber, location, modality) <> ((CustomerProfile.apply _).tupled, CustomerProfile.unapply)
}

object CustomerProfiles extends TableQueryWithId[CustomerProfile, CustomerProfiles](
  idLens = GenLens[CustomerProfile](_.customerId)
)(new CustomerProfiles(_))