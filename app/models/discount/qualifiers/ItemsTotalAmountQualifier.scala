package models.discount.qualifiers

import cats.data.Xor
import failures._
import failures.DiscountFailures._
import models.discount._
import services.Result
import utils.aliases._

case class ItemsTotalAmountQualifier(totalAmount: Int, search: ProductSearch) extends Qualifier {

  val qualifierType: QualifierType = ItemsTotalAmount

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Unit] =
    search.query(input).map {
      case Xor.Right(buckets) ⇒
        val matchedProductFormIds = buckets.filter(_.docCount > 0).map(_.key)
        if (totalAmount >= totalByProducts(input.lineItems, matchedProductFormIds)) Xor.Right(Unit)
        rejectXor(input, "Total amount is less than required")
      case _ ⇒
        Xor.Left(SearchFailure.single)
    }
}
