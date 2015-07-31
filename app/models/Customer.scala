package models

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import services.{Failures, Failure}
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import utils.GenericTable.TableWithId
import utils.{ModelWithIdParameter, TableQueryWithId, Validation, RichTable}
import com.wix.accord.Validator
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}
import org.scalactic._

final case class Customer(id: Int = 0, disabled: Boolean = false, email: String, password: String, firstName: String,
  lastName: String, phoneNumber: Option[String] = None, location: Option[String] = None,
  modality: Option[String] = None)
  extends Validation[Customer]
  with ModelWithIdParameter {

  override def validator = createValidator[Customer] { user =>
    user.firstName is notEmpty
    user.lastName is notEmpty
    user.email is notEmpty
  }
}

class Customers(tag: Tag) extends TableWithId[Customer](tag, "customers") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def disabled = column[Boolean]("disabled")
  def disabledBy = column[Option[Int]]("disabled_by")
  def email = column[String]("email")
  def password = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def phoneNumber = column[Option[String]]("phone_number")
  def location = column[Option[String]]("location")
  def modality = column[Option[String]]("modality")

  def * = (id, disabled, email, password, firstName, lastName,
    phoneNumber, location, modality) <> ((Customer.apply _).tupled, Customer.unapply)
}

object Customers extends TableQueryWithId[Customer, Customers](
  idLens = GenLens[Customer](_.id)
  )(new Customers(_)){

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): Future[Option[Customer]] = {
    db.run(filter(_.email === email).result.headOption)
  }

  def findById(id: Int)(implicit db: Database): Future[Option[Customer]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { filter(_.id === id) }

  def createFromPayload(payload: payloads.CreateCustomer)
                       (implicit ec: ExecutionContext, db: Database): Future[Customer Or Failures] = {
    val newCustomer = Customer(id = 0, email = payload.email,password = payload.password,
      firstName = payload.firstName, lastName = payload.firstName)

    save(newCustomer).run().map(Good(_))
  }
}

