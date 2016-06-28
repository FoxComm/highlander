package models.order.lineitems

import cats.data.Xor
import com.pellucid.sealerate
import models.inventory.Sku
import models.order.Order
import models.order.lineitems.OrderLineItem._
import shapeless._
import failures.Failures
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils._
import utils.db._

case class OrderLineItem(id: Int = 0,
                         referenceNumber: String = "",
                         orderRef: String,
                         originId: Int,
                         originType: OriginType = OrderLineItem.SkuItem,
                         state: State = Cart)
    extends FoxModel[OrderLineItem]
    with FSM[OrderLineItem.State, OrderLineItem] {

  import OrderLineItem._

  def stateLens = lens[OrderLineItem].state
  override def updateTo(newModel: OrderLineItem): Failures Xor OrderLineItem =
    super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
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
  sealed trait State
  case object Cart        extends State
  case object Pending     extends State
  case object PreOrdered  extends State
  case object BackOrdered extends State
  case object Canceled    extends State
  case object Shipped     extends State

  object State extends ADT[State] {
    def types = sealerate.values[State]
  }

  sealed trait OriginType
  case object SkuItem      extends OriginType
  case object GiftCardItem extends OriginType

  object OriginType extends ADT[OriginType] {
    def types = sealerate.values[OriginType]
  }

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
  implicit val originTypeColumnType: JdbcType[OriginType] with BaseTypedType[OriginType] =
    OriginType.slickColumn

  def buildGiftCard(order: Order, origin: OrderLineItemGiftCard): OrderLineItem = {
    OrderLineItem(
        orderRef = order.refNum,
        originId = origin.id,
        originType = GiftCardItem,
        state = Cart
    )
  }

  def buildSku(order: Order, sku: Sku): OrderLineItem = {
    OrderLineItem(
        orderRef = order.refNum,
        originId = sku.id,
        originType = SkuItem,
        state = Cart
    )
  }
}

class OrderLineItems(tag: Tag) extends FoxTable[OrderLineItem](tag, "order_line_items") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def orderRef        = column[String]("order_ref")
  def originId        = column[Int]("origin_id")
  def originType      = column[OrderLineItem.OriginType]("origin_type")
  def state           = column[OrderLineItem.State]("state")
  def * =
    (id, referenceNumber, orderRef, originId, originType, state) <>
      ((OrderLineItem.apply _).tupled, OrderLineItem.unapply)

  def skuLineItems = foreignKey(OrderLineItemSkus.tableName, originId, OrderLineItemSkus)(_.id)
}

object OrderLineItems
    extends FoxTableQuery[OrderLineItem, OrderLineItems](new OrderLineItems(_))
    with ReturningId[OrderLineItem, OrderLineItems] {

  val returningLens: Lens[OrderLineItem, Int] = lens[OrderLineItem].id

  import scope._

  def findByOrderRef(orderRef: Rep[String]): Query[OrderLineItems, OrderLineItem, Seq] =
    filter(_.orderRef === orderRef)

  def countByOrder(order: Order): DBIO[Int] = findByOrderRef(order.refNum).length.result

  def countBySkuIdForOrder(order: Order): Query[(Rep[Int], Rep[Int]), (Int, Int), Seq] =
    for {
      (skuId, group) ← findByOrderRef(order.refNum).skuItems.groupBy(_.originId)
    } yield (skuId, group.length)

  object scope {
    implicit class OriginTypeQuerySeqConversions(q: QuerySeq) {
      def giftCards: QuerySeq = q.byOriginType(GiftCardItem)
      def skuItems: QuerySeq  = q.byOriginType(SkuItem)

      def byOriginType(originType: OriginType): QuerySeq =
        q.filter(_.originType === (originType: OriginType))
    }
  }
}
