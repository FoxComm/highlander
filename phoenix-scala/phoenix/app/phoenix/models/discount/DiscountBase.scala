package phoenix.models.discount

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def unitsByProducts(lineItems: Seq[DqLineItem], productIds: Seq[String]): Int =
    lineItems.foldLeft(0) { (sum, data) ⇒
      if (productIds.contains(data.productId.toString)) sum + 1 else sum
    }

  def totalByProducts(lineItems: Seq[DqLineItem], productIds: Seq[String]): Long =
    lineItems.foldLeft(0L) { (sum, data) ⇒
      if (productIds.contains(data.productId.toString)) sum + data.price else sum
    }
}
