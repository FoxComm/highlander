package payloads

import com.pellucid.sealerate
import java.time.Instant
import models.activity.ActivityContext
import utils.aliases._
import utils.{ADT, ADTTypeHints}

case class LoginPayload(email: String, password: String, org: String)

case class UpdateShippingMethod(shippingMethodId: Int)

case class NotificationActivity(id: String,
                                kind: String,
                                data: Json,
                                context: ActivityContext,
                                createdAt: Instant)

case class CreateNotification(sourceDimension: String,
                              sourceObjectId: String,
                              activity: NotificationActivity)

sealed trait ExportEntity {
  def fields: List[String]

  def description: Option[String]
}
object ExportEntity {
  def typeHints =
    ADTTypeHints(
        Map(
            Type.Id    → classOf[UsingIDs],
            Type.Query → classOf[UsingSearchQuery]
        ))

  sealed trait Type extends Product with Serializable
  implicit object Type extends ADT[Type] {
    case object Id    extends Type
    case object Query extends Type

    def types: Set[Type] = sealerate.values[Type]
  }

  case class UsingIDs(fields: List[String], description: Option[String], ids: List[Long])
      extends ExportEntity
  case class UsingSearchQuery(fields: List[String], description: Option[String], query: Json)
      extends ExportEntity
}
