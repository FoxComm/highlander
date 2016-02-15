package models

import scala.concurrent.ExecutionContext

import cats.data.ValidatedNel
import cats.implicits._
import models.order.OrderAssignments
import services.Failure
import utils.Litterbox._
import utils.Validation
import utils.Slick.implicits._

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class StoreAdmin(id: Int = 0, email: String, password: String, name: String, department: Option[String] = None)
  extends ModelWithIdParameter[StoreAdmin]
  with Validation[StoreAdmin] {

  import Validation._

  override def validate: ValidatedNel[Failure, StoreAdmin] = {
    ( notEmpty(name, "name")
      |@| notEmpty(email, "email")
      ).map { case _ ⇒ this }
  }
}

class StoreAdmins(tag: Tag) extends GenericTable.TableWithId[StoreAdmin](tag, "store_admins")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("hashed_password")
  def name = column[String]("name")
  def department = column[Option[String]]("department")

  def * = (id, email, password, name, department) <> ((StoreAdmin.apply _).tupled, StoreAdmin.unapply)

  def assignedOrders = OrderAssignments.filter(_.assigneeId === id).flatMap(_.order)
}

object StoreAdmins extends TableQueryWithId[StoreAdmin, StoreAdmins](
  idLens = GenLens[StoreAdmin](_.id)
)(new StoreAdmins(_)){

  def findByEmail(email: String)(implicit ec: ExecutionContext, db: Database): DBIO[Option[StoreAdmin]] = {
    filter(_.email === email).one
  }
}
