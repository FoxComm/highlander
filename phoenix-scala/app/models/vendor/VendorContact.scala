package models.Vendor

import java.time.Instant
import slick.driver.PostgresDriver.api._
import utils.db._
import utils.Validation
import shapeless._

case class VendorContact(id: Int = 0,
                         email: Option[String] = None,
                         name: Option[String] = None,
                         description: Option[String] = None,
                         department: Option[String] = None,
                         locationId: Option[Int] = None,
                         phoneNumber: Option[String] = None,
                         vendorAdminId: Option[Int] = None,
                         createdAt: Instant = Instant.now)
    extends FoxModel[VendorContact]
    with Validation[VendorContact] {

  import Validation._

  //Validate something here.    
}

object VendorContact {}

class VendorContacts(tag: Tag) extends FoxTable[VendorContact](tag, "vendor_contacts") {
  def id            = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email         = column[Option[String]]("email")
  def name          = column[Option[String]]("name")
  def description   = column[Option[String]]("description")
  def department    = column[Option[String]]("department")
  def location      = column[Option[Int]]("location_id")
  def phoneNumber   = column[Option[String]]("phone_number")
  def vendorAdminId = column[Option[Int]]("vendor_admin_id")
  def createdAt     = column[Instant]("created_at")

  def * =
    (id, email, name, description, department, location, phoneNumber, vendorAdminId, createdAt) <> ((VendorContact.apply _).tupled, VendorContact.unapply)
}

object VendorContacts
    extends FoxTableQuery[VendorContact, VendorContacts](new VendorContacts(_))
    with ReturningId[VendorContact, VendorContacts] {

  val returningLens: Lens[VendorContact, Int] = lens[VendorContact].id
}
