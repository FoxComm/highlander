package phoenix.models.discount

/**
  * Methods, used across offers and qualifiers
  */
trait DiscountBase {

  def unitsByProducts(lineItems: Seq[DqLineItem], productIds: Seq[String]): Int =
    byProducts(lineItems, productIds).size

  def totalByProducts(lineItems: Seq[DqLineItem], productIds: Seq[String]): Long =
    byProducts(lineItems, productIds).map(_.price).sum

  private def byProducts(lineItems: Seq[DqLineItem], productIds: Seq[String]): Seq[DqLineItem] =
    lineItems.filter(productIds contains _.productId.toString)

}
