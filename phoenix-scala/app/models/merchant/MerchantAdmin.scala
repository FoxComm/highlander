package models.Merchant

import models.Merchant._
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



case class MerchantAdmin (id: Int = 0,
                        email: Option[String] = None,
                        hashedPassword: Option[String] = None,
                        isDisabled: Boolean = false,
                        disabledBy: Option[Int] = None,
                        createdAt: Instant = Instant.now)
    extends FoxModel[MerchantAdmin]
    with Validation[MerchantAdmin] {

  import Validation._

  override def validate: ValidatedNel[Failure, MerchantAdmin] = {
    (notEmpty(name, "name") |@| notEmpty(email, "email")).map { case _ ⇒ this }
  }

}

/* TODO: We should generalize the notion of Admin, then apply it via composition 
 * to MerchantAdmin and StoreAdmin.
 */
object MerchantAdmin {

  def build(id: Int = 0,
            email: String,
            password: Option[String] = None,
            isDisabled: Boolean = false,
            disabledBy: Option[Int] = None,
            createdAt: Instant = Instant.now): MerchantAdmin = {
  val passwordHash = password.map(hashPassword)
  MerchantAdmin(id = id,
              hashedPassword = passwordHash,
              isDisabled = isDisabled,
              disabledBy = disabledBy,
              createdAt = createdAt)
  }
}

class MerchantAdmins(tag: Tag) extends FoxTable[MerchantAdmin](tag, "merchant_admins") { 
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def hashedPassword = column[Option[String]]("hashed_password")
  def isDisabled = column[Boolean]("is_disabled")
  def disabledBy = column[Option[Int]]("disabled_by")
  def createdAt = column[Instant]("created_at")

  def * = (id, 
           email, 
           hashedPassword, 
           isDisabled, 
           disabledBy,
           createdAt) <> ((MerchantAdmin.apply _).tupled, MerchantAdmin.unapply)
}

object MerchantAdmins
    extends FoxTableQuery[MerchantAdmin, MerchantAdmins](new MerchantAdmins(_))
    with ReturningId[MerchantAdmin, MerchantAdmins] {
    
  val returningLens: Lens[MerchantAdmin, Int] = lens[MerchantAdmin].id
}
