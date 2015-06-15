package models

import com.wix.accord.dsl.{validator => createValidator}
import payloads.CreateCustomerPayload
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils.{Validation, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import org.scalactic._

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
  val returningId = table.returning(table.map(_.id))

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] = {
    db.run(table.filter(_.email === email).result.headOption)
  }

  def findById(id: Int)(implicit db: Database): Future[Option[Customer]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { table.filter(_.id === id) }

  def createFromPayload(payload: CreateCustomerPayload)
                       (implicit ec: ExecutionContext, db: Database): Future[Customer Or List[ErrorMessage]] = {
    val newCustomer = Customer(id = 0, email = payload.email, password = payload.password, firstName = payload.firstName, lastName = payload.firstName)

    db.run(for {
      customerId <- returningId += newCustomer
    } yield Good(newCustomer.copy(id = customerId)))
  }
}

