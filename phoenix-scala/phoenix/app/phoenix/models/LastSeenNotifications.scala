package phoenix.models

import com.github.tminglei.slickpg.LTree
import core.db.ExPostgresDriver.api._
import core.db._
import phoenix.models.account._
import shapeless._
import slick.lifted.Tag

case class LastSeenNotification(id: Int = 0, scope: LTree, accountId: Int, notificationId: Int)
    extends FoxModel[LastSeenNotification] {}

class LastSeenNotifications(tag: Tag) extends FoxTable[LastSeenNotification](tag, "last_seen_notifications") {

  def id             = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scope          = column[LTree]("scope")
  def accountId      = column[Int]("account_id")
  def notificationId = column[Int]("notification_id")

  def * =
    (id, scope, accountId, notificationId) <> ((LastSeenNotification.apply _).tupled,
    LastSeenNotification.unapply)

  def account = foreignKey(Accounts.tableName, accountId, Accounts)(_.id)
}

object LastSeenNotifications
    extends FoxTableQuery[LastSeenNotification, LastSeenNotifications](new LastSeenNotifications(_))
    with ReturningId[LastSeenNotification, LastSeenNotifications] {

  val returningLens: Lens[LastSeenNotification, Int] = lens[LastSeenNotification].id

  def findByScopeAndAccountId(scope: LTree, accountId: Int): QuerySeq =
    filter(_.scope === scope).filter(_.accountId === accountId)

}
