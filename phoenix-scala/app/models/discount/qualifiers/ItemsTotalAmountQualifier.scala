package models.discount.qualifiers

import scala.util.Try

import cats.data.Xor
import failures.DiscountFailures._
import failures._
import models.discount._
import utils.ElasticsearchApi._
import utils.aliases._
import utils.db._

case class ItemsTotalAmountQualifier(totalAmount: Int, search: Seq[ProductSearch])
    extends Qualifier
    with ItemsQualifier
    with NonEmptySearch {

  val qualifierType: QualifierType = ItemsTotalAmount

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    checkInner(input)(search)

  def matchXor(input: DiscountInput)(xor: Failures Xor Buckets): Failures Xor Unit = xor match {
    case Xor.Right(buckets) ⇒
      val matchedProductFormIds =
        buckets.filter(_.docCount > 0).flatMap(b ⇒ Try(b.key.toInt).toOption).toSet
      if (totalAmount >= totalByProducts(input.lineItems, matchedProductFormIds)) Xor.Right(Unit)
      rejectXor(input, "Total amount is less than required")
    case _ ⇒
      Xor.Left(SearchFailure.single)
  }
}
