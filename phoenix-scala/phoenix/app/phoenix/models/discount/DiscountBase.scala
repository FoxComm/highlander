package phoenix.models.discount

import phoenix.models.cord.lineitems.LineItemProductData

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def unitsByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (formIds.contains(data.productForm.id.toString)) sum + 1 else sum
    }

  def totalByProducts(lineItems: Seq[LineItemProductData[_]], formIds: Seq[String]): Long =
    lineItems.foldLeft(0L) { (sum, data) ⇒
      if (formIds.contains(data.productForm.id.toString)) sum + data.price else sum
    }
}
