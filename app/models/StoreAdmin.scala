package models

import scala.concurrent.ExecutionContext

import cats.data.ValidatedNel
import cats.implicits._
import models.order.OrderAssignments
import services.Failure
import utils.Litterbox._
import utils.Passwords.hashPassword
import utils.Validation
import utils.Slick.implicits._

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreAdmin(id: Int = 0, email: String, hashedPassword: String, name: String,
  department: Option[String] = None)
  extends ModelWithIdParameter[StoreAdmin]
  with Validation[StoreAdmin] {

  import Validation._

  override def validate: ValidatedNel[Failure, StoreAdmin] = {
    ( notEmpty(name, "name")
      |@| notEmpty(email, "email")
      ).map { case _ ⇒ this }
  }
}

object StoreAdmin {
  def build(id: Int = 0, email: String, password: String, name: String,
    department: Option[String] = None): StoreAdmin = {
    val passwordHash = hashPassword(password)
    StoreAdmin(id = id, email = email, hashedPassword = passwordHash, name = name, department = department)
  }
}

class StoreAdmins(tag: Tag) extends GenericTable.TableWithId[StoreAdmin](tag, "store_admins")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def hashedPassword = column[String]("hashed_password")
  def name = column[String]("name")
  def department = column[Option[String]]("department")

  def * = (id, email, hashedPassword, name, department) <> ((StoreAdmin.apply _).tupled, StoreAdmin.unapply)

  def assignedOrders = OrderAssignments.filter(_.assigneeId === id).flatMap(_.order)
}

object StoreAdmins extends TableQueryWithId[StoreAdmin, StoreAdmins](
  idLens = GenLens[StoreAdmin](_.id)
)(new StoreAdmins(_)){

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): DBIO[Option[StoreAdmin]] = {
    filter(_.email === email).one
  }
}
