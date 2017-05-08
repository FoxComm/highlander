package models.discount.qualifiers

import cats.implicits._
import failures.DiscountFailures._
import failures._
import models.discount._
import utils.ElasticsearchApi._
import utils.aliases._
import utils.apis.Apis
import utils.db._

case class ItemsAnyQualifier(search: Seq[ProductSearch])
    extends Qualifier
    with NonEmptySearch
    with ItemsQualifier {

  val qualifierType: QualifierType = ItemsAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    checkInner(input)(search)

  def matchEither(input: DiscountInput)(
      either: Either[Failures, Buckets]): Either[Failures, Unit] = either match {
    case Right(buckets) ⇒
      val bucketDocCount = buckets.foldLeft(0.toLong)((acc, bucket) ⇒ acc + bucket.docCount)
      if (bucketDocCount > 0) Either.right(Unit) else Either.left(SearchFailure.single)
    case _ ⇒
      Either.left(SearchFailure.single)
  }
}
