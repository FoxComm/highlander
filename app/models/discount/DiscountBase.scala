package models.discount

import models.order.lineitems.OrderLineItemProductData
import models.product.Mvp

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def price(data: OrderLineItemProductData): Int = Mvp.priceAsInt(data.skuForm, data.skuShadow)

  def unitsByProducts(lineItems: Seq[OrderLineItemProductData], formIds: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (formIds.contains(data.product.formId.toString)) sum + 1 else sum
    }

  def totalByProducts(lineItems: Seq[OrderLineItemProductData], formIds: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (formIds.contains(data.product.formId.toString)) sum + price(data) else sum
    }

  def unitsBySku(lineItems: Seq[OrderLineItemProductData], codes: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (codes.contains(data.sku.code)) sum + 1 else sum
    }

  def totalBySku(lineItems: Seq[OrderLineItemProductData], codes: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (codes.contains(data.sku.code)) sum + price(data) else sum
    }
}
