package phoenix.models.discount

import phoenix.models.cord.lineitems.LineItemProductData
import phoenix.models.product.Mvp

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def price[A](data: LineItemProductData[A]): Long =
    Mvp.priceAsAmount(data.skuForm, data.skuShadow)

  def unitsByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (formIds.contains(data.productForm.id.toString)) sum + 1 else sum
    }

  def totalByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Seq[String]): Long =
    lineItems.foldLeft(0L) { (sum, data) ⇒
      if (formIds.contains(data.productForm.id.toString)) sum + price(data) else sum
    }

  def unitsBySku(lineItems: Seq[LineItemProductData[_]], codes: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (codes.contains(data.sku.code)) sum + 1 else sum
    }

  def totalBySku(lineItems: Seq[LineItemProductData[_]], codes: Seq[String]): Long =
    lineItems.foldLeft(0L) { (sum, data) ⇒
      if (codes.contains(data.sku.code)) sum + price(data) else sum
    }
}
