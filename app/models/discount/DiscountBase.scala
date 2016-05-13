package models.discount

import models.order.lineitems.OrderLineItemProductData
import models.product.Mvp

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def price(data: OrderLineItemProductData): Int = Mvp.priceAsInt(data.skuForm, data.skuShadow)

  def unitsByProduct(lineItems: Seq[OrderLineItemProductData], formId: Int): Int = lineItems.foldLeft(0) { (sum, data) ⇒
    if (data.product.formId == formId) sum + 1 else sum
  }

  def unitsBySku(lineItems: Seq[OrderLineItemProductData], code: String): Int = lineItems.foldLeft(0) { (sum, data) ⇒
    if (data.sku.code == code) sum + 1 else sum
  }

  def totalByProduct(lineItems: Seq[OrderLineItemProductData], formId: Int): Int = lineItems.foldLeft(0) { (sum, data) ⇒
    if (data.product.formId == formId) sum + price(data) else sum
  }

  def totalBySku(lineItems: Seq[OrderLineItemProductData], code: String): Int = lineItems.foldLeft(0) { (sum, data) ⇒
    if (data.sku.code == code) sum + price(data) else sum
  }
}