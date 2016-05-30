package models.discount.qualifiers

import cats.data.Xor
import failures._
import failures.DiscountFailures._
import models.discount._
import services.Result
import utils.aliases._

case class ItemsNumUnitsQualifier(numUnits: Int, search: ProductSearch) extends Qualifier {

  val qualifierType: QualifierType = ItemsNumUnits

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES): Result[Unit] = {
    val future = for { result ← search.query(input) } yield result

    Result.fromFuture(
        future.map {
      case Xor.Right(count) if count > 0 ⇒ checkInner(input, search)
      case _                             ⇒ Xor.Left(SearchFailure)
    })
  }

  // FIXME
  private def checkInner(input: DiscountInput, search: ProductSearch): Xor[Failures, Unit] =
    if (numUnits >= unitsByProduct(input.lineItems, search.productSearchId)) Xor.Right(Unit)
    else rejectXor(input, "Number of units is less than required")
}
