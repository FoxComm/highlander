package models

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils.{Validation, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._

import scala.concurrent.{ExecutionContext, Future}

case class StoreAdmin(id: Int, email: String, password: String, firstName: String, lastName: String, department: Option[String]) extends Validation {
  override def validator[T] = {
    createValidator[StoreAdmin] { user =>
      user.firstName is notEmpty
      user.lastName is notEmpty
      user.email is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

class StoreAdmins(tag: Tag) extends Table[StoreAdmin](tag, "store_admins") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def department = column[Option[String]]("department")

  def * = (id, email, password, firstName, lastName, department) <> ((StoreAdmin.apply _).tupled, StoreAdmin.unapply)
}

object StoreAdmins {
  var table = TableQuery[StoreAdmins]

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): Future[Option[StoreAdmin]] = {
    db.run(table.filter(_.email === email).result.headOption)
  }
}