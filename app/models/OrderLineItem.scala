package models

import scala.concurrent.{ExecutionContext, Future}

import com.pellucid.sealerate
import models.OrderLineItem.{Cart, Status, GiftCardItem, SkuItem, OriginType}
import monocle.macros.GenLens
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils._

final case class OrderLineItem(id: Int = 0, orderId: Int, originId: Int,
  originType: OriginType = OrderLineItem.SkuItem, status: Status = Cart)
  extends ModelWithIdParameter
  with FSM[OrderLineItem.Status, OrderLineItem] {

  import OrderLineItem._

  def stateLens = GenLens[OrderLineItem](_.status)

  val fsm: Map[Status, Set[Status]] = Map(
    Cart →
      Set(Pending, PreOrdered, BackOrdered, Canceled),
    Pending →
      Set(Shipped, Canceled),
    PreOrdered →
      Set(Shipped, Canceled),
    BackOrdered →
      Set(Shipped, Canceled)
  )
}

object OrderLineItem {
  sealed trait Status
  case object Cart extends Status
  case object Pending extends Status
  case object PreOrdered extends Status
  case object BackOrdered extends Status
  case object Canceled extends Status
  case object Shipped extends Status

  object Status extends ADT[Status] {
    def types = sealerate.values[Status]
  }

  sealed trait OriginType
  case object SkuItem extends OriginType
  case object GiftCardItem extends OriginType

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  implicit val statusColumnType: JdbcType[Status] with BaseTypedType[Status] = Status.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] = OriginType.slickColumn

  def buildGiftCard(order: Order, origin: OrderLineItemGiftCard): OrderLineItem = {
    OrderLineItem(
      orderId = order.id,
      originId = origin.id,
      originType = GiftCardItem,
      status = Cart
    )
  }
}


class OrderLineItems(tag: Tag) extends GenericTable.TableWithId[OrderLineItem](tag, "order_line_items")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Int]("order_id")
  def originId = column[Int]("origin_id")
  def originType = column[OrderLineItem.OriginType]("origin_type")
  def status = column[OrderLineItem.Status]("status")
  def * = (id, orderId, originId, originType, status) <> ((OrderLineItem.apply _).tupled, OrderLineItem.unapply)

}

object OrderLineItems extends TableQueryWithId[OrderLineItem, OrderLineItems](
  idLens = GenLens[OrderLineItem](_.id)
)(new OrderLineItems(_)) {

  import scope._

  def findByOrder(order: Order)(implicit db: Database): Future[Seq[OrderLineItem]] =
    db.run(findByOrderId(order.id).result)

  def _findByOrder(order: Order): Query[OrderLineItems, OrderLineItem, Seq] = findByOrderId(order.id)

  def findByOrderId(orderId: Rep[Int]): Query[OrderLineItems, OrderLineItem, Seq] =
    filter(_.orderId === orderId)

  def countByOrder(order: Order): DBIO[Int] = findByOrderId(order.id).length.result

  def countBySkuIdForOrder(order: Order): DBIO[Seq[(Int, Int)]] =
    (for {
      (skuId, group) <- findByOrderId(order.id).skuItems.groupBy(_.originId)
    } yield (skuId, group.length)).result

  object scope {
    implicit class OriginTypeQuerySeqConversions(q: QuerySeq) {
      def giftCards: QuerySeq = q.byOriginType(GiftCardItem)
      def skuItems: QuerySeq  = q.byOriginType(SkuItem)

      def byOriginType(originType: OriginType): QuerySeq = filter(_.originType === (originType: OriginType))
    }
  }
}
