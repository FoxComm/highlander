package models

import com.wix.accord.dsl.{validator => createValidator}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils.{Validation, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

case class Customer(id: Int, email: String, password: String,
                    firstName: String, lastName: String) extends Validation[Customer] {
  override def validator = createValidator[Customer] { user =>
    user.firstName is notEmpty
    user.lastName is notEmpty
    user.email is notEmpty
  }
}

class Customers(tag: Tag) extends Table[Customer](tag, "customers") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")

  def * = (id, email, password, firstName, lastName) <> ((Customer.apply _).tupled, Customer.unapply)
}

object Customers {
  val table = TableQuery[Customers]

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] = {
    db.run(table.filter(_.email === email).result.headOption)
  }

  def findById(id: Int)(implicit db: Database): Future[Option[Customer]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { table.filter(_.id === id) }
}

