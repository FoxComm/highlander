package models

import com.pellucid.sealerate
import slick.dbio.Effect.Read
import slick.profile.FixedSqlStreamingAction
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class Note(id: Int = 0, storeAdminId: Int, referenceId: Int, referenceType: Note.ReferenceType, body: String)
  extends ModelWithIdParameter

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

  def author = foreignKey("store_admins", storeAdminId, TableQuery[StoreAdmins])(_.id) // what does this do? =]
}

object Notes extends TableQueryWithId[Note, Notes](
  idLens = GenLens[Note](_.id)
)(new Notes(_)) {

  def filterByOrderId(id: Int)
    (implicit ec: ExecutionContext, db:Database): Future[Seq[Note]] =
    _filterByOrderId(id).result.run()

  def _filterByOrderId(id: Int): Query[Notes, Note, Seq] =
    _filterByType(Note.Order).filter(_.referenceId === id)

  private [this] def _filterByType(referenceType: Note.ReferenceType) = filter(_.referenceType === referenceType)
}
