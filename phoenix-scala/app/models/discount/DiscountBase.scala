package models.discount

import models.cord.lineitems.LineItemProductData
import models.objects.ObjectForm
import models.product.Mvp

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def price(data: LineItemProductData[_]): Int =
    Mvp.priceAsInt(data.productVariantForm, data.productVariantShadow)

  def unitsByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Set[ObjectForm#Id]): Int =
    lineItems.count(formIds contains _.productForm.id)

  def totalByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Set[ObjectForm#Id]): Int =
    lineItems.filter(formIds contains _.productForm.id).map(price).sum

  def unitsBySku(lineItems: Seq[LineItemProductData[_]], codes: Set[String]): Int =
    lineItems.count(codes contains _.productVariant.code)

  def totalBySku(lineItems: Seq[LineItemProductData[_]], codes: Seq[String]): Int =
    lineItems.filter(codes contains _.productVariant.code).map(price).sum

}
