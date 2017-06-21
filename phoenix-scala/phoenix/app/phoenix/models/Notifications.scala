package phoenix.models

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import core.db.ExPostgresDriver.api._
import core.db._
import phoenix.models.account._
import phoenix.utils.aliases._
import shapeless._
import slick.lifted.Tag

case class Notification(id: Int = 0,
                        scope: LTree,
                        accountId: Int,
                        dimensionId: Int,
                        objectId: String,
                        activity: Json,
                        createdAt: Instant = Instant.now)
    extends FoxModel[Notification]

class Notifications(tag: Tag) extends FoxTable[Notification](tag, "notifications") {

  def id          = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope       = column[LTree]("scope")
  def accountId   = column[Int]("account_id")
  def dimensionId = column[Int]("dimension_id")
  def objectId    = column[String]("object_id")
  def activity    = column[Json]("activity")
  def createdAt   = column[Instant]("created_at")

  def * =
    (id, scope, accountId, dimensionId, objectId, activity, createdAt) <> ((Notification.apply _).tupled,
    Notification.unapply)

  def account = foreignKey(Accounts.tableName, accountId, Accounts)(_.id)
}

object Notification {
  def notificationChannel(adminId: Int): String = s"notifications_for_admin_$adminId"
}

object Notifications
    extends FoxTableQuery[Notification, Notifications](new Notifications(_))
    with ReturningId[Notification, Notifications] {

  val returningLens: Lens[Notification, Int] = lens[Notification].id

  def findByScopeAndAccountId(scope: LTree, accountId: Int): QuerySeq =
    filter(_.scope === scope).filter(_.accountId === accountId)

}
