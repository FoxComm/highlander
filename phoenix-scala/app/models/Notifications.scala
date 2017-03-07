package models

import java.time.Instant

import com.github.tminglei.slickpg.LTree

import com.pellucid.sealerate
import models.activity.Dimensions
import models.account._
import shapeless._
import slick.ast.BaseTypedType
import utils.db.ExPostgresDriver.api._
import slick.jdbc.JdbcType
import slick.lifted.Tag
import utils.db._
import utils.aliases._
import utils.ADT

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
  def notificationChannel(adminId: Int) = s"notifications_for_admin_$adminId"
}

object Notifications
    extends FoxTableQuery[Notification, Notifications](new Notifications(_))
    with ReturningId[Notification, Notifications] {

  val returningLens: Lens[Notification, Int] = lens[Notification].id

  def findByScopeAndAccountId(scope: LTree, accountId: Int): QuerySeq =
    filter(_.scope === scope).filter(_.accountId === accountId)

}
