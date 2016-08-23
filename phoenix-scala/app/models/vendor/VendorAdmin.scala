package models.Vendor

import java.time.Instant
import models.Vendor._
import cats.data.ValidatedNel
import cats.implicits._
import utils.aliases._
import cats.data.ValidatedNel
import cats.implicits._
import com.pellucid.sealerate
import failures.Failure
import utils.Passwords.hashPassword
import utils.{ADT, FSM, Validation}
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.aliases._
import utils.db._
import models.StoreAdmin._

case class VendorAdmin(id: Int = 0,
                       email: String,
                       hashedPassword: Option[String] = None,
                       isDisabled: Boolean = false,
                       disabledBy: Option[Int] = None,
                       createdAt: Instant = Instant.now)
    extends FoxModel[VendorAdmin]
    with Validation[VendorAdmin] {

  import Validation._

  override def validate: ValidatedNel[Failure, VendorAdmin] = {
    (notEmpty(email, "email")).map { case _ ⇒ this }
  }

}

/* TODO: We should generalize the notion of Admin, then apply it via composition 
 * to VendorAdmin and StoreAdmin.
 */
object VendorAdmin {

  def build(id: Int = 0,
            email: String,
            password: Option[String] = None,
            isDisabled: Boolean = false,
            disabledBy: Option[Int] = None,
            createdAt: Instant = Instant.now): VendorAdmin = {
    val passwordHash = password.map(hashPassword)
    VendorAdmin(id = id,
                email = email,
                hashedPassword = passwordHash,
                isDisabled = isDisabled,
                disabledBy = disabledBy,
                createdAt = createdAt)
  }
}

class VendorAdmins(tag: Tag) extends FoxTable[VendorAdmin](tag, "vendor_admins") {
  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email          = column[String]("email")
  def hashedPassword = column[Option[String]]("hashed_password")
  def isDisabled     = column[Boolean]("is_disabled")
  def disabledBy     = column[Option[Int]]("disabled_by")
  def createdAt      = column[Instant]("created_at")

  def * =
    (id, email, hashedPassword, isDisabled, disabledBy, createdAt) <> ((VendorAdmin.apply _).tupled, VendorAdmin.unapply)
}

object VendorAdmins
    extends FoxTableQuery[VendorAdmin, VendorAdmins](new VendorAdmins(_))
    with ReturningId[VendorAdmin, VendorAdmins] {

  val returningLens: Lens[VendorAdmin, Int] = lens[VendorAdmin].id
}
