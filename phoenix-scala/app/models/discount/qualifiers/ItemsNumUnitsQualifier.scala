package models.discount.qualifiers

import cats.implicits._
import failures.DiscountFailures._
import failures._
import io.circe.syntax._
import models.discount._
import utils.ElasticsearchApi._
import utils.aliases._
import utils.apis.Apis
import utils.db._
import utils.json.codecs._

case class ItemsNumUnitsQualifier(numUnits: Int, search: Seq[ProductSearch])
    extends Qualifier
    with NonEmptySearch
    with ItemsQualifier {

  val qualifierType: QualifierType = ItemsNumUnits

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    checkInner(input)(search)

  def matchEither(input: DiscountInput)(xor: Either[Failures, Buckets]): Either[Failures, Unit] =
    xor match {
      case Right(buckets) ⇒
        val matchedProductFormIds = buckets.filter(_.docCount > 0).map(_.key)
        if (numUnits >= unitsByProducts(input.lineItems, matchedProductFormIds)) Either.right(Unit)
        rejectEither(input, "Number of units is less than required")
      case _ ⇒
        Either.left(SearchFailure.single)
    }

  def json: Json = this.asJson
}
