package models

import cats.data.{ValidatedNel}
import utils.Validation.{notEmpty ⇒ notEmptyNew, lesserThanOrEqual ⇒ lesserThanOrEqualNew}

import com.pellucid.sealerate
import utils.{ADT, GenericTable, TableQueryWithId, ModelWithIdParameter, RichTable}

import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}

import scala.concurrent.{ExecutionContext, Future}

final case class Note(id: Int = 0, storeAdminId: Int, referenceId: Int, referenceType: Note.ReferenceType, body: String)
  extends ModelWithIdParameter {

  def validateNew: ValidatedNel[String, Customer] = {
    ( notEmptyNew(body, "body")
      |@| lesserThanOrEqualNew(body.length, 1000, "bodySize")
      ).map { case _ ⇒ this }
  }
}

object Note {
  sealed trait ReferenceType
  case object Order extends ReferenceType

  object ReferenceType extends ADT[ReferenceType] {
    def types = sealerate.values[ReferenceType]
  }

  implicit val noteColumnType = ReferenceType.slickColumn
}

class Notes(tag: Tag) extends GenericTable.TableWithId[Note](tag, "notes") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeAdminId = column[Int]("store_admin_id")
  def referenceId = column[Int]("reference_id")
  def referenceType = column[Note.ReferenceType]("reference_type")
  def body = column[String]("body")

  def * = (id, storeAdminId, referenceId, referenceType, body) <> ((Note.apply _).tupled, Note.unapply)

  def author = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object Notes extends TableQueryWithId[Note, Notes](
  idLens = GenLens[Note](_.id)
)(new Notes(_)) {

  def filterByOrderId(id: Int)
    (implicit ec: ExecutionContext, db:Database): Future[Seq[Note]] =
    _filterByOrderId(id).result.run()

  def _filterByOrderId(id: Int): Query[Notes, Note, Seq] =
    _filterByType(Note.Order).filter(_.referenceId === id)

  def _filterByIdAndAdminId(id: Int, adminId: Int): Query[Notes, Note, Seq] =
    filter(_.id === id).filter(_.storeAdminId === adminId)

  private [this] def _filterByType(referenceType: Note.ReferenceType) = filter(_.referenceType === referenceType)
}
