package models

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import utils.{Validation, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._

case class User(id: Int, email: String, password: String, firstName: String, lastName: String) extends Validation {
  override def validator[T] = {
    createValidator[User] { user =>
      user.firstName is notEmpty
      user.lastName is notEmpty
      user.email is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}

class Users(tag: Tag) extends Table[User](tag, "accounts") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def hashedPassword = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")

  def * = (id, email, hashedPassword, firstName, lastName) <> ((User.apply _).tupled, User.unapply)
}

object User {}

// TODO(yax): we should be extending Accounts or User or something
case class Shopper(id: Int, email: String, password: String, firstName: String, lastName: String) extends Validation {
  override def validator[T] = {
    createValidator[Shopper] { shopper =>
      shopper.firstName is notEmpty
      shopper.lastName is notEmpty
      shopper.email is notEmpty
    }
  }.asInstanceOf[Validator[T]] // TODO: fix me
}
