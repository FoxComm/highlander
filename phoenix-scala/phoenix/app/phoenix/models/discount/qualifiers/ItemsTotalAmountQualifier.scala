package phoenix.models.discount.qualifiers

import cats.implicits._
import core.db._
import core.failures._
import phoenix.failures.DiscountFailures._
import phoenix.models.discount._
import phoenix.utils.ElasticsearchApi._
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class ItemsTotalAmountQualifier(totalAmount: Long, search: Seq[ProductSearch])
    extends Qualifier
    with ItemsQualifier
    with NonEmptySearch {

  val qualifierType: QualifierType = ItemsTotalAmount

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    checkInner(input)(search)

  def matchEither(input: DiscountInput)(xor: Either[Failures, Buckets]): Either[Failures, Unit] =
    xor match {
      case Right(buckets) ⇒
        val matchedProductFormIds = buckets.filter(_.docCount > 0).map(_.key)
        if (totalAmount >= totalByProducts(input.lineItems, matchedProductFormIds))
          Either.right(Unit)
        rejectEither(input, "Total amount is less than required")
      case _ ⇒
        Either.left(SearchFailure.single)
    }
}
