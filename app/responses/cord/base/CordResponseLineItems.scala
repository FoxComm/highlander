package responses.cord.base

import models.cord.lineitems._
import models.product.Mvp
import responses.{GiftCardResponse, ResponseItem}
import slick.driver.PostgresDriver.api._
import utils.aliases._

case class CordResponseLineItem(imagePath: String,
                                referenceNumber: String,
                                name: String,
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
  val noImage = "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/no_image.jpg"

  def fetch(cordRef: String)(implicit ec: EC): DBIO[CordResponseLineItems] = {
    val liQ = OrderLineItemSkus
      .findLineItemsByCordRef(cordRef)
      .result
      .map(_.map { lineItems ⇒
        val data  = (OrderLineItemProductData.apply _).tupled(lineItems)
        val price = Mvp.priceAsInt(data.skuForm, data.skuShadow)
        val name  = Mvp.name(data.skuForm, data.skuShadow).getOrElse("")
        val image = Mvp.firstImage(data.skuForm, data.skuShadow).getOrElse(noImage)

        CordResponseLineItem(imagePath = image,
                             sku = data.sku.code,
                             referenceNumber = data.lineItem.referenceNumber,
                             state = data.lineItem.state,
                             name = name,
                             price = price,
                             productFormId = data.product.formId,
                             totalPrice = price)
      })

    val gcLiQ = OrderLineItemGiftCards
      .findLineItemsByCordRef(cordRef)
      .result
      .map(_.map { case (gc, li) ⇒ GiftCardResponse.build(gc) })

    for {
      skuList ← liQ
      gcList  ← gcLiQ
    } yield CordResponseLineItems(skus = skuList, giftCards = gcList)
  }
}

case class CordResponseLineItemAdjustment(
    adjustmentType: OrderLineItemAdjustment.AdjustmentType,
    substract: Int,
    lineItemRefNum: Option[String]
) extends ResponseItem

object CordResponseLineItemAdjustments {

  def fetch(cordRef: String)(implicit ec: EC): DBIO[Seq[CordResponseLineItemAdjustment]] =
    OrderLineItemAdjustments
      .findByCordRef(cordRef)
      .result
      .map(_.map { model ⇒
        CordResponseLineItemAdjustment(adjustmentType = model.adjustmentType,
                                       substract = model.substract,
                                       lineItemRefNum = model.lineItemRefNum)
      })
}
