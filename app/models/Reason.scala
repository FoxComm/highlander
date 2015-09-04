package models

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Validation

import com.wix.accord.dsl.{validator ⇒ createValidator, _}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class Reason(id: Int = 0, storeAdminId: Int, body: String, parentId: Option[Int] = None)
  extends ModelWithIdParameter {

  def validate: ValidatedNel[Failure, Reason] = {
    ( Validation.notEmpty(body, "body")
      |@| Validation.lesserThanOrEqual(body.length, 255, "bodySize")
      ).map { case _ ⇒ this }
  }

  def isSubReason: Boolean = parentId.isDefined
}

object Reason {
}

class Reasons(tag: Tag) extends GenericTable.TableWithId[Reason](tag, "reasons")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeAdminId = column[Int]("store_admin_id")
  def parentId = column[Option[Int]]("parent_id")
  def body = column[String]("body")

  def * = (id, storeAdminId, body, parentId) <> ((Reason.apply _).tupled, Reason.unapply)

  def author = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object Reasons extends TableQueryWithId[Reason, Reasons](
  idLens = GenLens[Reason](_.id)
)(new Reasons(_)) {
}
