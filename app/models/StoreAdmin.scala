package models

import scala.concurrent.{ExecutionContext, Future}

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Validation
import utils.Slick.implicits._

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreAdmin(id: Int = 0, email: String, password: String,
                      firstName: String, lastName: String,
                      department: Option[String] = None)
  extends ModelWithIdParameter {

  def validate: ValidatedNel[Failure, StoreAdmin] = {
    ( Validation.notEmpty(firstName, "firstName")
      |@| Validation.notEmpty(lastName, "lastName")
      |@| Validation.notEmpty(email, "email")
      ).map { case _ ⇒ this }
  }
}

class StoreAdmins(tag: Tag) extends GenericTable.TableWithId[StoreAdmin](tag, "store_admins")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("hashed_password")
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def department = column[Option[String]]("department")

  def * = (id, email, password, firstName, lastName, department) <> ((StoreAdmin.apply _).tupled, StoreAdmin.unapply)
}

object StoreAdmins extends TableQueryWithId[StoreAdmin, StoreAdmins](
  idLens = GenLens[StoreAdmin](_.id)
)(new StoreAdmins(_)){

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): Future[Option[StoreAdmin]] = {
    db.run(filter(_.email === email).one)
  }

  def findById(id: Int)(implicit db: Database): Future[Option[StoreAdmin]] = {
    db.run(_findById(id).extract.one)
  }

  def _findById(id: Rep[Int]) = { filter(_.id === id) }
}
