package models

import com.pellucid.sealerate
import utils.{ADT, GenericTable, Validation, TableQueryWithId, ModelWithIdParameter, RichTable}

import com.wix.accord.dsl.{validator => createValidator}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}
import org.scalactic._
import com.wix.accord.{Failure => ValidationFailure, Validator}
import com.wix.accord.dsl._
import scala.concurrent.{ExecutionContext, Future}

final case class Note(id: Int = 0, storeAdminId: Int, referenceId: Int, referenceType: Note.Type, text: String)
  extends ModelWithIdParameter

object Note {
  sealed trait Type
  case object Order extends Type

  object Type extends ADT[Type] {
    def types = sealerate.values[Type]
  }

  implicit val noteColumnType = Type.slickColumn
}

class Notes(tag: Tag) extends GenericTable.TableWithId[Note](tag, "notes") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def storeAdminId = column[Int]("store_admin_id")
  def referenceId = column[Int]("reference_id")
  def referenceType = column[Note.Type]("reference_type")
  def text = column[String]("text")

  def * = (id, storeAdminId, referenceId, referenceType, text) <> ((Note.apply _).tupled, Note.unapply)

  def author = foreignKey("store_admins", storeAdminId, TableQuery[StoreAdmins])(_.id) // what does this do? =]
}

object Notes extends TableQueryWithId[Note, Notes](
  idLens = GenLens[Note](_.id)
)(new Notes(_)) {

  def filterByOrderId(id: Int)
    (implicit ec: ExecutionContext, db:Database): Future[Option[Seq[Note]]] =
    _filterByOrderId(id).run().map(Some(_))

  def _filterByOrderId(id: Int) =
    filter(_.referenceId === id).filter(_.referenceType === Order.Note).result
}
