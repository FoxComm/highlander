package models.cord.lineitems

import cats.data.Xor
import com.pellucid.sealerate
import failures.Failures
import models.cord.Cart
import models.cord.lineitems.OrderLineItem.{GiftCardItem, OriginType, SkuItem}
import models.cord.lineitems.{OrderLineItem ⇒ OLI}
import models.inventory.Sku
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils._
import utils.db._

case class OrderLineItem(id: Int = 0,
                         referenceNumber: String = "",
                         cordRef: String,
                         originId: Int,
                         originType: OLI.OriginType = OLI.SkuItem,
                         state: OLI.State = OLI.Cart)
    extends FoxModel[OrderLineItem]
    with FSM[OrderLineItem.State, OrderLineItem] {

  import OrderLineItem._

  def stateLens = lens[OrderLineItem].state

  override def updateTo(newModel: OrderLineItem): Failures Xor OrderLineItem =
    super.transitionModel(newModel)

  val fsm: Map[State, Set[State]] = Map(
      OLI.Cart →
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

  def buildGiftCard(cart: Cart, origin: OrderLineItemGiftCard): OrderLineItem = {
    OrderLineItem(
        cordRef = cart.refNum,
        originId = origin.id,
        originType = GiftCardItem,
        state = Cart
    )
  }

  def buildSku(cart: Cart, sku: Sku): OrderLineItem = {
    OrderLineItem(
        cordRef = cart.refNum,
        originId = sku.id,
        originType = SkuItem,
        state = Cart
    )
  }
}

class OrderLineItems(tag: Tag) extends FoxTable[OrderLineItem](tag, "order_line_items") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def cordRef         = column[String]("cord_ref")
  def originId        = column[Int]("origin_id")
  def originType      = column[OrderLineItem.OriginType]("origin_type")
  def state           = column[OrderLineItem.State]("state")
  def * =
    (id, referenceNumber, cordRef, originId, originType, state) <>
      ((OrderLineItem.apply _).tupled, OrderLineItem.unapply)

  def skuLineItems = foreignKey(OrderLineItemSkus.tableName, originId, OrderLineItemSkus)(_.id)
}

object OrderLineItems
    extends FoxTableQuery[OrderLineItem, OrderLineItems](new OrderLineItems(_))
    with ReturningId[OrderLineItem, OrderLineItems] {

  val returningLens: Lens[OrderLineItem, Int] = lens[OrderLineItem].id

  import scope._

  def findByOrderRef(cordRef: Rep[String]): Query[OrderLineItems, OrderLineItem, Seq] =
    filter(_.cordRef === cordRef)

  def countByOrder(cart: Cart): DBIO[Int] = findByOrderRef(cart.refNum).length.result

  def countBySkuIdForCart(cart: Cart): Query[(Rep[Int], Rep[Int]), (Int, Int), Seq] =
    for {
      (skuId, group) ← findByOrderRef(cart.refNum).skuItems.groupBy(_.originId)
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
