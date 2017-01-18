package models

import java.time.Instant

import com.pellucid.sealerate
import models.activity.Dimensions
import models.account._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import slick.lifted.Tag
import utils.db._
import utils.ADT

case class NotificationSubscription(id: Int = 0,
                                    adminId: Int,
                                    dimensionId: Int,
                                    objectId: String,
                                    createdAt: Instant = Instant.now,
                                    reason: NotificationSubscription.Reason)
    extends FoxModel[NotificationSubscription] {}

object NotificationSubscription {
  sealed trait Reason
  case object Watching extends Reason
  case object Assigned extends Reason

  object Reason extends ADT[Reason] {
    def types = sealerate.values[Reason]
  }

  implicit val reasonColumnType: JdbcType[Reason] with BaseTypedType[Reason] = Reason.slickColumn
}

class NotificationSubscriptions(tag: Tag)
    extends FoxTable[NotificationSubscription](tag, "notification_subscriptions") {

  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def adminId     = column[Int]("admin_id")
  def dimensionId = column[Int]("dimension_id")
  def objectId    = column[String]("object_id")
  def createdAt   = column[Instant]("created_at")
  def reason      = column[NotificationSubscription.Reason]("reason")

  def * =
    (id, adminId, dimensionId, objectId, createdAt, reason) <> ((NotificationSubscription.apply _).tupled,
    NotificationSubscription.unapply)

  def dimension = foreignKey(Dimensions.tableName, dimensionId, Dimensions)(_.id)
  def admin     = foreignKey(Users.tableName, adminId, Users)(_.accountId)
}

object NotificationSubscriptions
    extends FoxTableQuery[NotificationSubscription, NotificationSubscriptions](
      new NotificationSubscriptions(_))
    with ReturningId[NotificationSubscription, NotificationSubscriptions] {

  val returningLens: Lens[NotificationSubscription, Int] = lens[NotificationSubscription].id

  def findByDimensionAndObject(dimensionId: Int, objectId: String): QuerySeq =
    filter(_.dimensionId === dimensionId).filter(_.objectId === objectId)

  def find(dimensionId: Int, objectId: String, adminId: Int): QuerySeq =
    findByDimensionAndObject(dimensionId = dimensionId, objectId = objectId)
      .filter(_.adminId === adminId)
}

object Notification {
  def notificationChannel(adminId: Int) = s"notifications_for_admin_$adminId"
}

case class NotificationTrailMetadata(lastSeenActivityId: Int)
