package phoenix.models.cord.lineitems

import cats.implicits._
import com.pellucid.sealerate
import core.db.ExPostgresDriver.api._
import core.db._
import core.failures.Failures
import objectframework.FormShadowGet
import objectframework.models._
import org.json4s.Extraction.decompose
import org.json4s.Formats
import phoenix.models.cord.lineitems.{OrderLineItem ⇒ OLI}
import phoenix.models.inventory.{Sku, Skus}
import phoenix.utils.aliases._
import phoenix.utils.{ADT, FSM, JsonFormatters}
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType

trait LineItemProductData[LI] {
  def sku: Sku
  def skuForm: ObjectForm
  def skuShadow: ObjectShadow
  def productForm: ObjectForm
  def productShadow: ObjectShadow
  def image: Option[String]
  def lineItem: LI
  def attributes: Option[LineItemAttributes]
  def lineItemReferenceNumber: String
  def lineItemState: OrderLineItem.State
  def withLineItemReferenceNumber(newLineItemRef: String): LineItemProductData[LI]

  def isGiftCard: Boolean = attributes.flatMap(_.giftCard).isDefined

  def price: Long = FormShadowGet.priceAsLong(skuForm, skuShadow)
}

case class OrderLineItemProductData(sku: Sku,
                                    skuForm: ObjectForm,
                                    skuShadow: ObjectShadow,
                                    productForm: ObjectForm,
                                    productShadow: ObjectShadow,
                                    image: Option[String],
                                    lineItem: OrderLineItem,
                                    attributes: Option[LineItemAttributes] = None)
    extends LineItemProductData[OrderLineItem] {
  def lineItemReferenceNumber = lineItem.referenceNumber
  def lineItemState           = lineItem.state
  def withLineItemReferenceNumber(newLineItemRef: String) =
    this.copy(lineItem = lineItem.copy(referenceNumber = newLineItemRef))
}

case class OrderLineItem(id: Int = 0,
                         referenceNumber: String = "",
                         cordRef: String,
                         skuId: Int,
                         skuShadowId: Int,
                         state: OLI.State = OLI.Cart,
                         attributes: Option[LineItemAttributes] = None)
    extends FoxModel[OrderLineItem]
    with FSM[OrderLineItem.State, OrderLineItem] {

  import OrderLineItem._

  def stateLens = lens[OrderLineItem].state

  override def updateTo(newModel: OrderLineItem): Either[Failures, OrderLineItem] =
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
  def attributes      = column[Option[Json]]("attributes")

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def * =
    (id, referenceNumber, cordRef, skuId, skuShadowId, state, attributes).shaped <>
      ({
        case (id, refNum, cordRef, skuId, skuShadowId, state, attrs) ⇒
          OrderLineItem(id,
                        refNum,
                        cordRef,
                        skuId,
                        skuShadowId,
                        state,
                        attrs.flatMap(_.extractOpt[LineItemAttributes]))
      }, { oli: OrderLineItem ⇒
        (oli.id,
         oli.referenceNumber,
         oli.cordRef,
         oli.skuId,
         oli.skuShadowId,
         oli.state,
         oli.attributes.map(decompose)).some
      })

  def sku    = foreignKey(Skus.tableName, skuId, Skus)(_.id)
  def shadow = foreignKey(ObjectShadows.tableName, skuShadowId, ObjectShadows)(_.id)
}

object OrderLineItems
    extends FoxTableQuery[OrderLineItem, OrderLineItems](new OrderLineItems(_))
    with ReturningId[OrderLineItem, OrderLineItems] {

  val returningLens: Lens[OrderLineItem, Int] = lens[OrderLineItem].id

  def findByOrderRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

  def findBySkuId(id: Int): DBIO[Option[OrderLineItem]] =
    filter(_.skuId === id).one

  object scope {
    implicit class OrderLineItemQuerySeqConversions(private val q: QuerySeq) extends AnyVal {
      def withSkus: Query[(OrderLineItems, Skus), (OrderLineItem, Sku), Seq] =
        for {
          items ← q
          skus  ← items.sku
        } yield (items, skus)

      def forContextAndCode(contextId: Int, code: String): QuerySeq =
        for {
          items ← q
          sku   ← items.sku if sku.code === code && sku.contextId === contextId
        } yield items
    }
  }

}
