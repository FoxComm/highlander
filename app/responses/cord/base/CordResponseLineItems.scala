package responses.cord.base

import models.cord.lineitems._
import models.product.Mvp
import responses.{GiftCardResponse, ResponseItem}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import CartLineItemSkus.scope._

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

case class CordResponseLineItems(skus: Seq[CordResponseLineItem] = Seq.empty,
                                 giftCards: Seq[GiftCardResponse.Root] = Seq.empty)
    extends ResponseItem

object CordResponseLineItems {

  type AdjustmentMap = Map[String, CordResponseLineItemAdjustment]

  def fetch(cordRef: String, adjustments: Seq[CordResponseLineItemAdjustment])(
      implicit ec: EC): DBIO[CordResponseLineItems] =
    fetch(cordRef, adjustments, cordLineItemsFromOrder)

  def fetchCart(cordRef: String, adjustments: Seq[CordResponseLineItemAdjustment])(
      implicit ec: EC): DBIO[CordResponseLineItems] =
    fetch(cordRef, adjustments, cordLineItemsFromCart)

  def fetch(cordRef: String,
            adjustments: Seq[CordResponseLineItemAdjustment],
            readLineItems: (String, AdjustmentMap) ⇒ DBIO[Seq[CordResponseLineItem]])(
      implicit ec: EC): DBIO[CordResponseLineItems] = {
    val adjustmentMap = mapAdjustments(adjustments)
    val liQ           = readLineItems(cordRef, adjustmentMap)

    val gcLiQ = OrderLineItemGiftCards
      .findLineItemsByCordRef(cordRef)
      .result
      .map(_.map { case (gc, li) ⇒ GiftCardResponse.build(gc) })

    for {
      skuList ← liQ
      gcList  ← gcLiQ
    } yield CordResponseLineItems(skus = skuList, giftCards = gcList)
  }

  def cordLineItemsFromOrder(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC): DBIO[Seq[CordResponseLineItem]] = {
    OrderLineItemSkus
      .findLineItemsByCordRef(cordRef)
      .result
      .map(
          //Convert to OrderLineItemProductData
          _.map(resultToData).map { data ⇒
            createResponse(data, 1)
          }.toSeq
      )
  }

  def cordLineItemsFromCart(cordRef: String, adjustmentMap: AdjustmentMap)(
      implicit ec: EC): DBIO[Seq[CordResponseLineItem]] = {
    CartLineItemSkus.byCordRef(cordRef).lineItems.result.map {
      //Convert to OrderLineItemProductData
      _.map(resultToCartData)
      //Group by adjustments/unadjusted
        .groupBy(lineItem ⇒ groupKey(lineItem, adjustmentMap))
        //Convert groups to responses.
        .map { case (_, lineItemGroup) ⇒ createResponseGrouped(lineItemGroup, adjustmentMap) }
        .toSeq
    }
  }

  private def resultToData(
      result: OrderLineItemSkus.FindLineItemResult): OrderLineItemProductData =
    (OrderLineItemProductData.apply _).tupled(result)

  private def resultToCartData(
      result: CartLineItemSkus.FindLineItemResult): CartLineItemProductData =
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

  private def createResponseGrouped(
      lineItemData: Seq[CartLineItemProductData],
      adjMap: Map[String, CordResponseLineItemAdjustment]): CordResponseLineItem = {

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

  private def createResponse(data: LineItemProductData[_], quantity: Int): CordResponseLineItem = {
    require(quantity > 0)

    val price = Mvp.priceAsInt(data.skuForm, data.skuShadow)
    val name  = Mvp.name(data.skuForm, data.skuShadow)
    val image = Mvp.firstImage(data.skuForm, data.skuShadow).getOrElse(NO_IMAGE)

    CordResponseLineItem(imagePath = image,
                         sku = data.sku.code,
                         referenceNumber = data.lineItemReferenceNumber,
                         state = data.lineItemState,
                         name = name,
                         price = price,
                         productFormId = data.product.formId,
                         totalPrice = price,
                         quantity = quantity)
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
