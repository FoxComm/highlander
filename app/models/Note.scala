package models

import cats.data.ValidatedNel
import cats.implicits._
import services.Failure
import utils.Litterbox._
import utils.Validation

import scala.concurrent.{ExecutionContext, Future}

import com.pellucid.sealerate
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, GenericTable, ModelWithIdParameter, TableQueryWithId, Validation}
import utils.Slick.implicits._

final case class Note(id: Int = 0, storeAdminId: Int, referenceId: Int, referenceType: Note.ReferenceType, body: String)
  extends ModelWithIdParameter
  with Validation[Note] {

  import Validation._

  def validate: ValidatedNel[Failure, Note] = {
    ( notEmpty(body, "body")
      |@| lesserThanOrEqual(body.length, 1000, "bodySize")
      ).map { case _ ⇒ this }
  }
}

object Note {
  sealed trait ReferenceType
  case object Order extends ReferenceType
  case object GiftCard extends ReferenceType

  object ReferenceType extends ADT[ReferenceType] {
    def types = sealerate.values[ReferenceType]
  }

  implicit val noteColumnType: JdbcType[ReferenceType] with BaseTypedType[ReferenceType] = ReferenceType.slickColumn
}

class Notes(tag: Tag) extends GenericTable.TableWithId[Note](tag, "notes")  {
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

  def filterByOrderId(id: Int): QuerySeq =
    filterByType(Note.Order).filter(_.referenceId === id)

  def filterByGiftCardId(id: Int): QuerySeq =
    filterByType(Note.GiftCard).filter(_.referenceId === id)

  def filterByIdAndAdminId(id: Int, adminId: Int): QuerySeq =
    filter(_.id === id).filter(_.storeAdminId === adminId)

  private [this] def filterByType(referenceType: Note.ReferenceType) = filter(_.referenceType === referenceType)
}
