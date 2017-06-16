package phoenix.models

import cats.data.ValidatedNel
import cats.implicits._
import com.pellucid.sealerate
import core.db._
import core.failures.Failure
import core.utils.Validation
import phoenix.models.Reason.{General, ReasonType}
import phoenix.models.account._
import phoenix.utils.ADT
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import slick.jdbc.PostgresProfile.api._

case class Reason(id: Int = 0,
                  reasonType: ReasonType = General,
                  storeAdminId: Int,
                  body: String,
                  parentId: Option[Int] = None)
    extends FoxModel[Reason]
    with Validation[Reason] {

  import Validation._

  override def validate: ValidatedNel[Failure, Reason] =
    (notEmpty(body, "body") |@| lesserThanOrEqual(body.length, 255, "bodySize")).map {
      case _ ⇒ this
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

  def author = foreignKey(Users.tableName, storeAdminId, Users)(_.accountId)
}

object Reasons extends FoxTableQuery[Reason, Reasons](new Reasons(_)) with ReturningId[Reason, Reasons] {

  val returningLens: Lens[Reason, Int] = lens[Reason].id

}
