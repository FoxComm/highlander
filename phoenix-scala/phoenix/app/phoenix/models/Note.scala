package models

import java.time.Instant

import models.account._
import cats.data.ValidatedNel
import cats.implicits._
import com.github.tminglei.slickpg.LTree
import com.pellucid.sealerate
import shapeless._
import failures.Failure
import payloads.NotePayloads.CreateNote
import slick.ast.BaseTypedType
import utils.db.ExPostgresDriver.api._
import slick.jdbc.JdbcType
import utils.{ADT, Validation}
import utils.db._

case class Note(id: Int = 0,
                scope: LTree,
                storeAdminId: Int,
                referenceId: Int,
                referenceType: Note.ReferenceType,
                body: String,
                createdAt: Instant = Instant.now,
                deletedAt: Option[Instant] = None,
                deletedBy: Option[Int] = None)
    extends FoxModel[Note]
    with Validation[Note] {

  import Validation._

  override def validate: ValidatedNel[Failure, Note] = {
    (notEmpty(body, "body") |@| lesserThanOrEqual(body.length, 1000, "bodySize")).map {
      case _ ⇒ this
    }
  }
}

object Note {
  sealed trait ReferenceType
  case object Order      extends ReferenceType
  case object GiftCard   extends ReferenceType
  case object Customer   extends ReferenceType
  case object Return     extends ReferenceType
  case object Sku        extends ReferenceType
  case object Product    extends ReferenceType
  case object Promotion  extends ReferenceType
  case object Coupon     extends ReferenceType
  case object StoreAdmin extends ReferenceType

  object ReferenceType extends ADT[ReferenceType] {
    def types = sealerate.values[ReferenceType]
  }

  implicit val noteColumnType: JdbcType[ReferenceType] with BaseTypedType[ReferenceType] =
    ReferenceType.slickColumn

}

class Notes(tag: Tag) extends FoxTable[Note](tag, "notes") {
  def id            = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope         = column[LTree]("scope")
  def storeAdminId  = column[Int]("store_admin_id")
  def referenceId   = column[Int]("reference_id")
  def referenceType = column[Note.ReferenceType]("reference_type")
  def body          = column[String]("body")
  def createdAt     = column[Instant]("created_at")
  def deletedAt     = column[Option[Instant]]("deleted_at")
  def deletedBy     = column[Option[Int]]("deleted_by")

  def * =
    (id, scope, storeAdminId, referenceId, referenceType, body, createdAt, deletedAt, deletedBy) <> ((Note.apply _).tupled, Note.unapply)

  def author = foreignKey(Users.tableName, storeAdminId, Users)(_.accountId)
}

object Notes extends FoxTableQuery[Note, Notes](new Notes(_)) with ReturningId[Note, Notes] {

  val returningLens: Lens[Note, Int] = lens[Note].id

  def filterByIdAndAdminId(id: Int, adminId: Int): QuerySeq =
    filter(_.id === id).filter(_.storeAdminId === adminId)

  object scope {
    implicit class NotesQuerySeqConversions(q: QuerySeq) {
      def notDeleted: QuerySeq =
        q.filterNot(note ⇒ note.deletedAt.isDefined || note.deletedBy.isDefined)
    }
  }
}
