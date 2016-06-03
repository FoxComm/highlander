package models

import cats.data.ValidatedNel
import cats.implicits._
import com.pellucid.sealerate
import failures.Failure
import models.Reason.{General, ReasonType}
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils.db._
import utils.http.CustomDirectives.SortAndPage
import utils.{ADT, Validation}

case class Reason(id: Int = 0,
                  reasonType: ReasonType = General,
                  storeAdminId: Int,
                  body: String,
                  parentId: Option[Int] = None)
    extends FoxModel[Reason]
    with Validation[Reason] {

  import Validation._

  override def validate: ValidatedNel[Failure, Reason] = {
    (notEmpty(body, "body") |@| lesserThanOrEqual(body.length, 255, "bodySize")).map {
      case _ ⇒ this
    }
  }

  def isSubReason: Boolean = parentId.isDefined
}

object Reason {
  sealed trait ReasonType
  case object General             extends ReasonType
  case object GiftCardCreation    extends ReasonType
  case object StoreCreditCreation extends ReasonType
  case object Cancellation        extends ReasonType

  object ReasonType extends ADT[ReasonType] {
    def types = sealerate.values[ReasonType]
  }

  val reasonTypeRegex = """([a-zA-Z]*)""".r

  implicit val reasonTypeColumnType: JdbcType[ReasonType] with BaseTypedType[ReasonType] =
    ReasonType.slickColumn
}

class Reasons(tag: Tag) extends FoxTable[Reason](tag, "reasons") {
  def id           = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def reasonType   = column[ReasonType]("reason_type")
  def storeAdminId = column[Int]("store_admin_id")
  def parentId     = column[Option[Int]]("parent_id")
  def body         = column[String]("body")

  def * =
    (id, reasonType, storeAdminId, body, parentId) <> ((Reason.apply _).tupled, Reason.unapply)

  def author = foreignKey(StoreAdmins.tableName, storeAdminId, StoreAdmins)(_.id)
}

object Reasons
    extends FoxTableQuery[Reason, Reasons](new Reasons(_))
    with ReturningId[Reason, Reasons] {

  val returningLens: Lens[Reason, Int] = lens[Reason].id

  def sortedAndPaged(query: QuerySeq)(implicit sortAndPage: SortAndPage): QuerySeqWithMetadata = {
    val sortedQuery = query.withMetadata.sortAndPageIfNeeded {
      case (s, reason) ⇒
        s.sortColumn match {
          case "id"           ⇒ if (s.asc) reason.id.asc else reason.id.desc
          case "reasonType"   ⇒ if (s.asc) reason.reasonType.asc else reason.reasonType.desc
          case "storeAdminId" ⇒ if (s.asc) reason.storeAdminId.asc else reason.storeAdminId.desc
          case "parentId"     ⇒ if (s.asc) reason.parentId.asc else reason.parentId.desc
          case "body"         ⇒ if (s.asc) reason.body.asc else reason.body.desc
          case other          ⇒ invalidSortColumn(other)
        }
    }

    sortedQuery.paged
  }
}
