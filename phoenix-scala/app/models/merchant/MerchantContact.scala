package models.Merchant

import java.time.Instant
import models.location._
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.Validation
import shapeless._
import com.pellucid.sealerate

case class MerchantContact(id: Int = 0,
                           email: Option[String] = None,
                           name: Option[String] = None,
                           description: Option[String] = None,
                           department: Option[String] = None,
                           locationId: Option[Int] = None,
                           phoneNumber: Option[String] = None,
                           merchantAdminId: Option[Int] = None,
                           createdAt: Instant = Instant.now)
    extends FoxModel[MerchantContact]
    with Validation[MerchantContact] {

  import Validation._

  //Validate something here.    
}

object MerchantContact {}

class MerchantContacts(tag: Tag) extends FoxTable[MerchantContact](tag, "merchant_contacts") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email           = column[Option[String]]("email")
  def name            = column[Option[String]]("name")
  def description     = column[Option[String]]("description")
  def department      = column[Option[String]]("department")
  def location        = column[Option[Int]]("location_id")
  def phoneNumber     = column[Option[String]]("phone_number")
  def merchantAdminId = column[Option[Int]]("merchant_admin_id")
  def createdAt       = column[Instant]("created_at")

  def * =
    (id, email, name, description, department, location, phoneNumber, merchantAdminId, createdAt) <> ((MerchantContact.apply _).tupled, MerchantContact.unapply)
}

object MerchantContacts
    extends FoxTableQuery[MerchantContact, MerchantContacts](new MerchantContacts(_))
    with ReturningId[MerchantContact, MerchantContacts] {

  val returningLens: Lens[MerchantContact, Int] = lens[MerchantContact].id
}
