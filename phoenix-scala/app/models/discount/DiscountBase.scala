package models.discount

import models.cord.lineitems.LineItemProductData
import models.product.Mvp

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def price[A](data: LineItemProductData[A]): Int =
    Mvp.priceAsInt(data.productVariantForm, data.productVariantShadow)

  def unitsByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (formIds.contains(data.productForm.id.toString)) sum + 1 else sum
    }

  def totalByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (formIds.contains(data.productForm.id.toString)) sum + price(data) else sum
    }

  def unitsBySku(lineItems: Seq[LineItemProductData[_]], codes: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (codes.contains(data.productVariant.code)) sum + 1 else sum
    }

  def totalBySku(lineItems: Seq[LineItemProductData[_]], codes: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (codes.contains(data.productVariant.code)) sum + price(data) else sum
    }
}
