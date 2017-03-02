package models.discount.qualifiers

import cats.data.Xor
import failures.DiscountFailures._
import failures._
import models.discount._
import utils.db._
import utils.ElasticsearchApi._
import utils.aliases._

case class ItemsAnyQualifier(search: Seq[ProductSearch])
    extends Qualifier
    with NonEmptySearch
    with ItemsQualifier {

  val qualifierType: QualifierType = ItemsAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    checkInner(input)(search)

  def matchXor(input: DiscountInput)(xor: Failures Xor Buckets): Failures Xor Unit = xor match {
    case Xor.Right(buckets) ⇒
      val bucketDocCount = buckets.map(_.docCount).sum
      if (bucketDocCount > 0L) Xor.Right(Unit) else Xor.Left(SearchFailure.single)
    case _ ⇒
      Xor.Left(SearchFailure.single)
  }
}
