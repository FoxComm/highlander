package phoenix.models.cord.lineitems

import java.time.Instant

import com.pellucid.sealerate
import core.db.ExPostgresDriver.api._
import core.db._
import phoenix.models.discount.offers.Offer.OfferResult
import phoenix.models.discount.offers._
import phoenix.utils.ADT
import shapeless._
import slick.ast.BaseTypedType
import slick.jdbc.JdbcType
import core.utils.Money._

final case class CartLineItemAdjustment(id: Int = 0,
                                        cordRef: String,
                                        promotionShadowId: Int,
                                        adjustmentType: CartLineItemAdjustment.AdjustmentType,
                                        subtract: Long,
                                        lineItemRefNum: Option[String] = None,
                                        createdAt: Instant = Instant.now)
    extends FoxModel[CartLineItemAdjustment]

class CartLineItemAdjustments(tag: Tag)
    extends FoxTable[CartLineItemAdjustment](tag, "cart_line_item_adjustments") {

  def id                = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cordRef           = column[String]("cord_ref")
  def promotionShadowId = column[Int]("promotion_shadow_id")
  def adjustmentType    = column[CartLineItemAdjustment.AdjustmentType]("adjustment_type")
  def subtract          = column[Long]("subtract")
  def lineItemRefNum    = column[Option[String]]("line_item_ref_num")
  def createdAt         = column[Instant]("created_at")

  def * =
    (id, cordRef, promotionShadowId, adjustmentType, subtract, lineItemRefNum, createdAt) <> ((CartLineItemAdjustment.apply _).tupled, CartLineItemAdjustment.unapply)
}

object CartLineItemAdjustment {
  sealed trait AdjustmentType
  case object LineItemAdjustment extends AdjustmentType
  case object OrderAdjustment    extends AdjustmentType
  case object ShippingAdjustment extends AdjustmentType
  case object Combinator         extends AdjustmentType

  def fromOfferResult(offerResult: OfferResult) =
    CartLineItemAdjustment(
      cordRef = offerResult.discountInput.cartRefNum,
      promotionShadowId = offerResult.discountInput.promotionShadowId,
      adjustmentType = adjustmentTypeByOffer(offerResult.offerType),
      subtract = offerResult.subtract,
      lineItemRefNum = offerResult.lineItemRefNum
    )

  def adjustmentTypeByOffer(offerType: OfferType): AdjustmentType = offerType match {
    case ItemPercentOff | ItemAmountOff    ⇒ LineItemAdjustment
    case ItemsPercentOff | ItemsAmountOff  ⇒ LineItemAdjustment
    case SetPrice                          ⇒ LineItemAdjustment
    case OrderPercentOff | OrderAmountOff  ⇒ OrderAdjustment
    case FreeShipping | DiscountedShipping ⇒ ShippingAdjustment
    case ListCombinator                    ⇒ Combinator
  }

  object AdjustmentType extends ADT[AdjustmentType] {
    def types = sealerate.values[AdjustmentType]
  }

  implicit val adjustmentTypeColumnType: JdbcType[AdjustmentType] with BaseTypedType[AdjustmentType] =
    AdjustmentType.slickColumn
}

object CartLineItemAdjustments
    extends FoxTableQuery[CartLineItemAdjustment, CartLineItemAdjustments](new CartLineItemAdjustments(_))
    with ReturningId[CartLineItemAdjustment, CartLineItemAdjustments] {

  val returningLens: Lens[CartLineItemAdjustment, Int] = lens[CartLineItemAdjustment].id

  def findByCordRef(cordRef: String): QuerySeq =
    filter(_.cordRef === cordRef)

  def filterByOrderRefAndShadow(cordRef: String, shadowId: Int): QuerySeq =
    findByCordRef(cordRef).filter(_.promotionShadowId === shadowId)

  def filterByOrderRefAndShadows(cordRef: String, shadows: Seq[Int]): QuerySeq =
    findByCordRef(cordRef).filter(_.promotionShadowId.inSet(shadows))
}
