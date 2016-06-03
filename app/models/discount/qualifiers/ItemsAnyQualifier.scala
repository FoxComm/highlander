package models.discount.qualifiers

import cats.data.Xor
import failures.DiscountFailures._
import models.discount._
import services.Result
import utils.aliases._

case class ItemsAnyQualifier(search: ProductSearch) extends Qualifier {

  val qualifierType: QualifierType = ItemsAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Unit] =
    search.query(input).map {
      case Xor.Right(buckets) ⇒
        val bucketDocCount = buckets.foldLeft(0.toLong)((acc, bucket) ⇒ acc + bucket.docCount)
        if (bucketDocCount > 0) Xor.Right(Unit) else Xor.Left(SearchFailure.single)
      case _ ⇒
        Xor.Left(SearchFailure.single)
    }
}
