package models

import scala.concurrent.{ExecutionContext, Future}

import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}

final case class StoreAdmin(id: Int = 0, email: String, password: String,
                      firstName: String, lastName: String,
                      department: Option[String] = None)
  extends ModelWithIdParameter
  with Validation[StoreAdmin] {
  override def validator = createValidator[StoreAdmin] { user =>
    user.firstName is notEmpty
    user.lastName is notEmpty
    user.email is notEmpty
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
    db.run(filter(_.email === email).result.headOption)
  }

  def findById(id: Int)(implicit db: Database): Future[Option[StoreAdmin]] = {
    db.run(_findById(id).result.headOption)
  }

  def _findById(id: Rep[Int]) = { filter(_.id === id) }
}
