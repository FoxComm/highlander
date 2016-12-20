package models.cord.lineitems

import cats.data.{ValidatedNel, Xor}
import cats.implicits._
import com.pellucid.sealerate
import failures.{Failure, Failures}
import models.cord.lineitems.{OrderLineItem ⇒ OLI}
import models.inventory.{ProductVariant, ProductVariants}
import models.objects._
import org.json4s.Extraction.decompose
import org.json4s.Formats
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import utils.Validation._
import utils._
import utils.aliases._
import utils.db.ExPostgresDriver.api._
import utils.db._

trait LineItemProductData[LI] {
  def variant: ProductVariant
  def variantForm: ObjectForm
  def variantShadow: ObjectShadow
  def productForm: ObjectForm
  def productShadow: ObjectShadow
  def image: Option[String]
  def lineItem: LI
  def attributes: Option[LineItemAttributes]
  def lineItemReferenceNumber: String
  def lineItemState: OrderLineItem.State
  def withLineItemReferenceNumber(newLineItemRef: String): LineItemProductData[LI]
}

case class OrderLineItemProductData(variant: ProductVariant,
                                    variantForm: ObjectForm,
                                    variantShadow: ObjectShadow,
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
                         variantId: Int,
                         variantShadowId: Int,
                         state: OLI.State = OLI.Cart,
                         attributes: Option[LineItemAttributes] = None)
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
  def variantId       = column[Int]("variant_id")
  def variantShadowId = column[Int]("variant_shadow_id")
  def state           = column[OrderLineItem.State]("state")
  def attributes      = column[Option[Json]]("attributes")

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def * =
    (id, referenceNumber, cordRef, variantId, variantShadowId, state, attributes).shaped <>
      ({
        case (id, refNum, cordRef, variantId, variantShadowId, state, attrs) ⇒
          OrderLineItem(id,
                        refNum,
                        cordRef,
                        variantId,
                        variantShadowId,
                        state,
                        attrs.flatMap(_.extractOpt[LineItemAttributes]))
      }, { oli: OrderLineItem ⇒
        (oli.id,
         oli.referenceNumber,
         oli.cordRef,
         oli.variantId,
         oli.variantShadowId,
         oli.state,
         oli.attributes.map(decompose)).some
      })

  def variant = foreignKey(ProductVariants.tableName, variantId, ProductVariants)(_.id)
  def shadow  = foreignKey(ObjectShadows.tableName, variantShadowId, ObjectShadows)(_.id)
}

object OrderLineItems
    extends FoxTableQuery[OrderLineItem, OrderLineItems](new OrderLineItems(_))
    with ReturningId[OrderLineItem, OrderLineItems] {

  val returningLens: Lens[OrderLineItem, Int] = lens[OrderLineItem].id

  def findByOrderRef(cordRef: Rep[String]): Query[OrderLineItems, OrderLineItem, Seq] =
    filter(_.cordRef === cordRef)

  def findByVariantId(id: Int): DBIO[Option[OrderLineItem]] =
    filter(_.variantId === id).one

  object scope {
    implicit class OrderLineItemQuerySeqConversions(q: QuerySeq) {
      def withVariants: Query[(OrderLineItems, ProductVariants),
                              (OrderLineItem, ProductVariant),
                              Seq] =
        for {
          items    ← q
          variants ← items.variant
        } yield (items, variants)
    }
  }

}
