package responses.cord.base

import models.cord.lineitems.CartLineItems.scope._
import models.cord.lineitems._
import models.product.Mvp
import responses.ResponseItem
import services.product.ProductManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

case class CordResponseLineItem(imagePath: String,
                                referenceNumber: String,
                                name: Option[String],
                                sku: String,
                                price: Int,
                                quantity: Int = 1,
                                totalPrice: Int,
                                productFormId: Int,
                                state: OrderLineItem.State)
    extends ResponseItem

case class CordResponseLineItems(skus: Seq[CordResponseLineItem] = Seq.empty) extends ResponseItem

object CordResponseLineItems {

  type AdjustmentMap = Map[String, CordResponseLineItemAdjustment]

  def fetch(cordRef: String, adjustments: Seq[CordResponseLineItemAdjustment])(
      implicit ec: EC,
      db: DB): DbResultT[CordResponseLineItems] =
    fetch(cordRef, adjustments, cordLineItemsFromOrder)

  def fetchCart(cordRef: String,
                adjustments: Seq[CordResponseLineItemAdjustment],
                grouped: Boolean)(implicit ec: EC, db: DB): DbResultT[CordResponseLineItems] =
    if (grouped) fetch(cordRef, adjustments, cordLineItemsFromCartGrouped)
    else fetch(cordRef, adjustments, cordLineItemsFromCart)

  def fetch(cordRef: String,
            adjustments: Seq[CordResponseLineItemAdjustment],
            readLineItems: (String, AdjustmentMap) ⇒ DbResultT[Seq[CordResponseLineItem]])(
      implicit ec: EC,
      db: DB): DbResultT[CordResponseLineItems] = {
    val adjustmentMap = mapAdjustments(adjustments)
    readLineItems(cordRef, adjustmentMap).map(skuList ⇒ CordResponseLineItems(skus = skuList))
  }

  def cordLineItemsFromOrder(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      li ← * <~ OrderLineItems.findLineItemsByCordRef(cordRef).result
      //Convert to OrderLineItemProductData
      result ← * <~ li
                .map(resultToData)
                .map { data ⇒
                  createResponse(data, 1)
                }
                .toSeq
    } yield result

  def cordLineItemsFromCartGrouped(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      lineItems ← * <~ CartLineItems.byCordRef(cordRef).lineItems.result
      //Convert to OrderLineItemProductData
      result ← * <~ lineItems
                .map(resultToCartData)
                //Group by adjustments/unadjusted
                .groupBy(lineItem ⇒ groupKey(lineItem, adjustmentMap))
                //Convert groups to responses.
                .map {
                  case (_, lineItemGroup) ⇒ createResponseGrouped(lineItemGroup, adjustmentMap)
                }
                .toSeq
    } yield result

  def cordLineItemsFromCart(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[CordResponseLineItem]] =
    for {
      lineItems ← * <~ CartLineItems.byCordRef(cordRef).lineItems.result
      result ← * <~ lineItems
                .map(resultToCartData)
                .map { data ⇒
                  createResponse(data, 1)
                }
                .toSeq
    } yield result

  private def resultToData(result: OrderLineItems.FindLineItemResult): OrderLineItemProductData =
    (OrderLineItemProductData.apply _).tupled(result)

  private def resultToCartData(result: CartLineItems.FindLineItemResult): CartLineItemProductData =
    (CartLineItemProductData.apply _).tupled(result)

  private val NOT_A_REF = "not_a_ref"

  private def mapAdjustments(adjustments: Seq[CordResponseLineItemAdjustment])
    : Map[String, CordResponseLineItemAdjustment] = {
    adjustments.map(a ⇒ a.lineItemRefNum.getOrElse(NOT_A_REF) → a).toMap
  }

  val NOT_ADJUSTED = "na"

  private def groupKey(data: CartLineItemProductData,
                       adjMap: Map[String, CordResponseLineItemAdjustment]): String = {
    val prefix = data.sku.id
    val suffix =
      if (adjMap.contains(data.lineItemReferenceNumber)) data.lineItemReferenceNumber
      else NOT_ADJUSTED
    s"$prefix,$suffix"
  }

  private val NO_IMAGE =
    "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg"

  private def createResponseGrouped(lineItemData: Seq[CartLineItemProductData],
                                    adjMap: Map[String, CordResponseLineItemAdjustment])(
      implicit ec: EC,
      db: DB): DbResultT[CordResponseLineItem] = {

    val data = lineItemData.head

    //only show reference number for line items that have adjustments.
    //This is because the adjustment list references the line item by the 
    //reference number. In the future it would be better if each line item
    //simply had a list of adjustments instead of the list sitting outside 
    //the line item.
    val referenceNumber =
      if (adjMap.contains(data.lineItemReferenceNumber))
        data.lineItemReferenceNumber
      else ""

    createResponse(data.copy(lineItem = data.lineItem.copy(referenceNumber = referenceNumber)),
                   lineItemData.length)
  }

  private def createResponse(data: LineItemProductData[_], quantity: Int)(
      implicit ec: EC,
      db: DB): DbResultT[CordResponseLineItem] = {
    require(quantity > 0)

    val price = Mvp.priceAsInt(data.skuForm, data.skuShadow)
    val name  = Mvp.name(data.skuForm, data.skuShadow)
    val image = Mvp.firstImage(data.skuForm, data.skuShadow).getOrElse(NO_IMAGE)

    val li = CordResponseLineItem(imagePath = image,
                                  sku = data.sku.code,
                                  referenceNumber = data.lineItemReferenceNumber,
                                  state = data.lineItemState,
                                  name = name,
                                  price = price,
                                  productFormId = data.product.formId,
                                  totalPrice = price,
                                  quantity = quantity)
    ProductManager.getFirstProductImageByFromId(data.product.formId).map {
      case Some(url) ⇒ li.copy(imagePath = url)
      case _         ⇒ li
    }
  }
}

case class CordResponseLineItemAdjustment(
    adjustmentType: OrderLineItemAdjustment.AdjustmentType,
    subtract: Int,
    lineItemRefNum: Option[String]
) extends ResponseItem

object CordResponseLineItemAdjustments {

  def fetch(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponseLineItemAdjustment]] =
    OrderLineItemAdjustments
      .findByCordRef(cordRef)
      .result
      .map(_.map { model ⇒
        CordResponseLineItemAdjustment(adjustmentType = model.adjustmentType,
                                       subtract = model.subtract,
                                       lineItemRefNum = model.lineItemRefNum)
      })
}
