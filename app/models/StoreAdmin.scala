package models

import cats.data.ValidatedNel
import cats.implicits._
import failures.Failure
import utils.Passwords.hashPassword
import utils.Validation
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class StoreAdmin(id: Int = 0, name: String, email: String, hashedPassword: Option[String] = None,
  department: Option[String] = None)
  extends FoxModel[StoreAdmin]
  with Validation[StoreAdmin] {

  import Validation._

  override def validate: ValidatedNel[Failure, StoreAdmin] = {
    ( notEmpty(name, "name")
      |@| notEmpty(email, "email")
      ).map { case _ ⇒ this }
  }
}

object StoreAdmin {
  def build(id: Int = 0, email: String, password: Option[String], name: String,
    department: Option[String] = None): StoreAdmin = {
    val passwordHash = password.map(hashPassword)
    StoreAdmin(id = id, email = email, hashedPassword = passwordHash, name = name, department = department)
  }
}

class StoreAdmins(tag: Tag) extends FoxTable[StoreAdmin](tag, "store_admins")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def hashedPassword = column[Option[String]]("hashed_password")
  def name = column[String]("name")
  def department = column[Option[String]]("department")

  def * = (id, name, email, hashedPassword, department) <> ((StoreAdmin.apply _).tupled, StoreAdmin.unapply)
}

object StoreAdmins extends FoxTableQuery[StoreAdmin, StoreAdmins](
  idLens = GenLens[StoreAdmin](_.id)
)(new StoreAdmins(_)){

  def findByEmail(email: String): DBIO[Option[StoreAdmin]] = {
    filter(_.email === email).one
  }
}
