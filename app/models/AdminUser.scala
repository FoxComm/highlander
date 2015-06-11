package models

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import utils.{Validation, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._

case class AdminUser(id: Int, email: String, password: String, firstName: String, lastName: String, department: String) extends Validation {
  override def validator[T] = {
    createValidator[AdminUser] { user =>
      user.firstName is notEmpty
      user.lastName is notEmpty
      user.email is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

class AdminUsers(tag: Tag) extends Table[AdminUser](tag, "admin_users") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def hashedPassword = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def department = column[String]("department")

  def * = (id, email, hashedPassword, firstName, lastName, department) <> ((AdminUser.apply _).tupled, AdminUser.unapply)
}

object AdminUser {}