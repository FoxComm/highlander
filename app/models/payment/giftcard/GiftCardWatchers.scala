package models.payment.giftcard

import java.time.Instant

import scala.concurrent.ExecutionContext

import models.{StoreAdmin, StoreAdmins, javaTimeSlickMapper}
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.lifted.Tag
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class GiftCardWatcher(id: Int = 0, giftCardId: Int, watcherId: Int, createdAt: Instant = Instant.now)
  extends ModelWithIdParameter[GiftCardWatcher]

object GiftCardWatcher

class GiftCardWatchers(tag: Tag) extends GenericTable.TableWithId[GiftCardWatcher](tag, "gift_card_watchers") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def giftCardId = column[Int]("gift_card_id")
  def watcherId = column[Int]("watcher_id")
  def createdAt = column[Instant]("created_at")

  def * = (id, giftCardId, watcherId, createdAt) <> ((GiftCardWatcher.apply _).tupled, GiftCardWatcher.unapply)
  def giftCard = foreignKey(GiftCards.tableName, giftCardId, GiftCards)(_.id)
  def watcher = foreignKey(StoreAdmins.tableName, watcherId, StoreAdmins)(_.id)
}

object GiftCardWatchers extends TableQueryWithId[GiftCardWatcher, GiftCardWatchers](
  idLens = GenLens[GiftCardWatcher](_.id)
)(new GiftCardWatchers(_)) {

  def byWatcher(admin: StoreAdmin): QuerySeq = filter(_.watcherId === admin.id)

  def watchingTo(admin: StoreAdmin)(implicit ec: ExecutionContext): GiftCards.QuerySeq = {
    for {
      watchers  ← byWatcher(admin).map(_.giftCardId)
      giftCards ← GiftCards.filter(_.id === watchers)
    } yield giftCards
  }

  def byGiftCard(order: GiftCard): QuerySeq = filter(_.giftCardId === order.id)

  def watchersFor(order: GiftCard)(implicit ec: ExecutionContext): StoreAdmins.QuerySeq = {
    for {
      watchers ← byGiftCard(order).map(_.watcherId)
      admins   ← StoreAdmins.filter(_.id === watchers)
    } yield admins
  }
}
