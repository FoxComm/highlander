package models.discount.qualifiers

import cats.data.Xor
import failures._
import failures.DiscountFailures._
import models.discount._
import services.Result
import utils.aliases._

case class ItemsNumUnitsQualifier(numUnits: Int, search: SearchReference) extends Qualifier {

  val qualifierType: QualifierType = ItemsNumUnits

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] = {
    val future = for { result ← SearchReference.query(input, search) } yield result

    Result.fromFuture(
        future.map {
      case Xor.Right(count) if count > 0 ⇒ checkInner(input, search)
      case _                             ⇒ Xor.Left(SearchFailure)
    })
  }

  private def checkInner(input: DiscountInput, search: SearchReference): Xor[Failures, Unit] =
    search match {
      case ProductSearch(formId) if numUnits >= unitsByProduct(input.lineItems, formId) ⇒
        Xor.Right(Unit)
      case SkuSearch(code) if numUnits >= unitsBySku(input.lineItems, code) ⇒ Xor.Right(Unit)
      case CustomerSearch(_)                                                ⇒ rejectXor(input, "Invalid search type")
      case _                                                                ⇒ rejectXor(input, "Number of units is less than required")
    }
}
