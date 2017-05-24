package phoenix.responses.cord.base

import core.db._
import phoenix.models.cord.lineitems._
import phoenix.models.product.Mvp
import phoenix.responses.ResponseItem
import phoenix.services.LineItemManager
import slick.jdbc.PostgresProfile.api._

case class CordResponseLineItem(imagePath: String,
                                referenceNumbers: Seq[String],
                                name: Option[String],
                                sku: String,
                                price: Int,
                                quantity: Int = 1,
                                totalPrice: Int,
                                productFormId: Int,
                                externalId: Option[String],
                                trackInventory: Boolean,
                                state: OrderLineItem.State,
                                attributes: Option[LineItemAttributes] = None)
    extends ResponseItem

case class CordResponseLineItems(skus: Seq[CordResponseLineItem] = Seq.empty) extends ResponseItem

object CordResponseLineItems {

  type AdjustmentMap = Map[String, CordResponseLineItemAdjustment]

  def fetch(cordRef: String, adjustments: Seq[CordResponseLineItemAdjustment], grouped: Boolean)(
      implicit ec: EC,
      db: DB): DbResultT[CordResponseLineItems] =
    if (grouped) fetchLineItems(cordRef, adjustments, cordLineItemsFromOrderGrouped)
    else fetchLineItems(cordRef, adjustments, cordLineItemsFromOrder)

  def fetchCart(cordRef: String,
                adjustments: Seq[CordResponseLineItemAdjustment],
                grouped: Boolean)(implicit ec: EC, db: DB): DbResultT[CordResponseLineItems] =
    if (grouped) fetchLineItems(cordRef, adjustments, cordLineItemsFromCartGrouped)
    else fetchLineItems(cordRef, adjustments, cordLineItemsFromCart)

  def fetchLineItems(cordRef: String,
                     adjustments: Seq[CordResponseLineItemAdjustment],
                     readLineItems: (String,
                                     AdjustmentMap) ⇒ DbResultT[Seq[CordResponseLineItem]])(
      implicit ec: EC,
      db: DB): DbResultT[CordResponseLineItems] = {
    val adjustmentMap = mapAdjustments(adjustments)
    readLineItems(cordRef, adjustmentMap).map(skuList ⇒ CordResponseLineItems(skus = skuList))
  }

  def cordLineItemsFromOrder(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      li     ← * <~ LineItemManager.getOrderLineItems(cordRef)
      result ← * <~ li.map(data ⇒ createResponse(data, Seq(data.lineItemReferenceNumber), 1))
    } yield result

  def cordLineItemsGrouped(lineItems: Seq[LineItemProductData[_]],
                           cordRef: String,
                           adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      _ ← * <~ lineItems.map(data ⇒ createResponse(data, Seq(data.lineItemReferenceNumber), 1))
      result ← * <~ lineItems
                .groupBy(lineItem ⇒ groupKey(lineItem, adjustmentMap, lineItem.attributes))
                .map {
                  case (_, lineItemGroup) ⇒ createResponseGrouped(lineItemGroup, adjustmentMap)
                }
                .toSeq
    } yield result

  def cordLineItemsFromOrderGrouped(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      lineItems ← * <~ LineItemManager.getOrderLineItems(cordRef)
      result    ← * <~ cordLineItemsGrouped(lineItems, cordRef, adjustmentMap)
    } yield result

  def cordLineItemsFromCartGrouped(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      lineItems ← * <~ LineItemManager.getCartLineItems(cordRef)
      result    ← * <~ cordLineItemsGrouped(lineItems, cordRef, adjustmentMap)
    } yield result

  def cordLineItemsFromCart(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      lineItems ← * <~ LineItemManager.getCartLineItems(cordRef)
      result ← * <~ lineItems.map(data ⇒
                    createResponse(data, Seq(data.lineItemReferenceNumber), 1))
    } yield result

  private val NOT_A_REF = "not_a_ref"

  private def mapAdjustments(adjustments: Seq[CordResponseLineItemAdjustment])
    : Map[String, CordResponseLineItemAdjustment] = {
    adjustments.map(a ⇒ a.lineItemRefNum.getOrElse(NOT_A_REF) → a).toMap
  }

  val NOT_ADJUSTED = "na"

  private def groupKey(data: LineItemProductData[_],
                       adjMap: Map[String, CordResponseLineItemAdjustment],
                       attributes: Option[LineItemAttributes] = None): String = {
    val prefix = data.sku.id + getAttributesHash(attributes)
    val suffix =
      if (adjMap.contains(data.lineItemReferenceNumber)) data.lineItemReferenceNumber
      else NOT_ADJUSTED
    s"$prefix,$suffix"
  }

  private def getAttributesHash(attributes: Option[LineItemAttributes]): String =
    attributes.fold("")(_.toString.hashCode.toString)

  val NO_IMAGE =
    "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg"

  private def createResponseGrouped(lineItemData: Seq[LineItemProductData[_]],
                                    adjMap: Map[String, CordResponseLineItemAdjustment])(
      implicit ec: EC,
      db: DB): CordResponseLineItem = {

    val data             = lineItemData.head
    val referenceNumbers = lineItemData.map(_.lineItemReferenceNumber)

    createResponse(data, referenceNumbers, lineItemData.length)
  }

  private def createResponse(data: LineItemProductData[_],
                             referenceNumbers: Seq[String],
                             quantity: Int)(implicit ec: EC, db: DB): CordResponseLineItem = {
    require(quantity > 0)

    val title = Mvp.title(data.productForm, data.productShadow)
    val image = data.image.getOrElse(NO_IMAGE)

    val price          = Mvp.priceAsInt(data.skuForm, data.skuShadow)
    val externalId     = Mvp.externalId(data.skuForm, data.skuShadow)
    val trackInventory = Mvp.trackInventory(data.skuForm, data.skuShadow)

    CordResponseLineItem(imagePath = image,
                         sku = data.sku.code,
                         referenceNumbers = Seq(data.lineItemReferenceNumber),
                         state = data.lineItemState,
                         name = title,
                         price = price,
                         externalId = externalId,
                         trackInventory = trackInventory,
                         productFormId = data.productForm.id,
                         totalPrice = price,
                         quantity = quantity,
                         attributes = data.attributes)
  }
}

case class CordResponseLineItemAdjustment(
    adjustmentType: CartLineItemAdjustment.AdjustmentType,
    subtract: Int,
    lineItemRefNum: Option[String]
) extends ResponseItem

object CordResponseLineItemAdjustments {

  def fetch(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponseLineItemAdjustment]] =
    CartLineItemAdjustments
      .findByCordRef(cordRef)
      .result
      .map(_.map { model ⇒
        CordResponseLineItemAdjustment(adjustmentType = model.adjustmentType,
                                       subtract = model.subtract,
                                       lineItemRefNum = model.lineItemRefNum)
      })
}
