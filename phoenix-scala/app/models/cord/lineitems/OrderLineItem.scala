package models.cord.lineitems

import cats.data.Xor
import com.pellucid.sealerate
import failures.Failures
import models.cord.Cart
import models.cord.lineitems.{OrderLineItem ⇒ OLI}
import models.inventory.{Sku, Skus}
import models.objects._
import models.product._
import shapeless._
import slick.ast.BaseTypedType
import slick.driver.PostgresDriver.api._
import slick.jdbc.JdbcType
import utils._
import utils.db._

trait LineItemProductData[LI] {
  def sku: Sku
  def skuForm: ObjectForm
  def skuShadow: ObjectShadow
  def productForm: ObjectForm
  def productShadow: ObjectShadow
  def lineItem: LI

  def lineItemReferenceNumber: String
  def lineItemState: OrderLineItem.State
}

case class OrderLineItemProductData(sku: Sku,
                                    skuForm: ObjectForm,
                                    skuShadow: ObjectShadow,
                                    productForm: ObjectForm,
                                    productShadow: ObjectShadow,
                                    lineItem: OrderLineItem)
    extends LineItemProductData[OrderLineItem] {
  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = lineItem.state
}

case class OrderLineItem(id: Int = 0,
                         referenceNumber: String = "",
                         cordRef: String,
                         skuId: Int,
                         skuShadowId: Int,
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

  implicit val stateColumnType: JdbcType[State] with BaseTypedType[State] = State.slickColumn
}

class OrderLineItems(tag: Tag) extends FoxTable[OrderLineItem](tag, "order_line_items") {
  def id              = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def referenceNumber = column[String]("reference_number")
  def cordRef         = column[String]("cord_ref")
  def skuId           = column[Int]("sku_id")
  def skuShadowId     = column[Int]("sku_shadow_id")
  def state           = column[OrderLineItem.State]("state")
  def * =
    (id, referenceNumber, cordRef, skuId, skuShadowId, state) <>
      ((OrderLineItem.apply _).tupled, OrderLineItem.unapply)

  def sku    = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, skuShadowId, ObjectShadows)(_.id)
}

object OrderLineItems
    extends FoxTableQuery[OrderLineItem, OrderLineItems](new OrderLineItems(_))
    with ReturningId[OrderLineItem, OrderLineItems] {

  val returningLens: Lens[OrderLineItem, Int] = lens[OrderLineItem].id

  import scope._

  def findByOrderRef(cordRef: Rep[String]): Query[OrderLineItems, OrderLineItem, Seq] =
    filter(_.cordRef === cordRef)

  def findBySkuId(id: Int): DBIO[Option[OrderLineItem]] =
    filter(_.skuId === id).one

  object scope {
    implicit class OrderLineItemQuerySeqConversions(q: QuerySeq) {
      def withSkus: Query[(OrderLineItems, Skus), (OrderLineItem, Sku), Seq] =
        for {
          items ← q
          skus  ← items.sku
        } yield (items, skus)
    }
  }

}
