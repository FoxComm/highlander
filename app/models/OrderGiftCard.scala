package models

import scala.concurrent.{Future, ExecutionContext}

import com.pellucid.sealerate
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils._

final case class OrderGiftCard(id: Int = 0, orderId: Int, giftCardId: Int) extends ModelWithIdParameter

object OrderGiftCard {
  def build(order: Order, gc: GiftCard): OrderGiftCard = OrderGiftCard(orderId = order.id, giftCardId = gc.id)
}

class OrderGiftCards(tag: Tag) extends GenericTable.TableWithId[OrderGiftCard](tag, "order_gift_cards")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def giftCardId = column[Int]("gift_card_id")
  def * = (id, orderId, giftCardId) <> ((OrderGiftCard.apply _).tupled, OrderGiftCard.unapply)
}

object OrderGiftCards extends TableQueryWithId[OrderGiftCard, OrderGiftCards](
  idLens = GenLens[OrderGiftCard](_.id)
)(new OrderGiftCards(_)) {

  def findByOrder(order: Order)(implicit db: Database) = db.run(_findByOrderId(order.id).result)

  def _findByOrder(order: Order): Query[OrderGiftCards, OrderGiftCard, Seq] = _findByOrderId(order.id)

  def _findByOrderId(orderId: Rep[Int]) = { filter(_.orderId === orderId) }
}
