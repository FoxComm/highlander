package phoenix.models.discount.qualifiers

import cats.implicits._
import phoenix.failures.DiscountFailures._
import core.failures._
import phoenix.models.discount._
import phoenix.utils.ElasticsearchApi._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import core.db._

case class ItemsAnyQualifier(search: Seq[ProductSearch])
    extends Qualifier
    with NonEmptySearch
    with ItemsQualifier {

  val qualifierType: QualifierType = ItemsAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    checkInner(input)(search)

  def matchEither(input: DiscountInput)(either: Either[Failures, Buckets]): Either[Failures, Unit] =
    either match {
      case Right(buckets) ⇒
        val bucketDocCount = buckets.map(_.docCount).sum
        if (bucketDocCount > 0) Either.right(Unit) else Either.left(SearchFailure.single)
      case _ ⇒
        Either.left(SearchFailure.single)
    }
}
