package models.discount.qualifiers

import cats.data.Xor
import failures.DiscountFailures._
import models.discount.{CustomerSearch, DiscountInput}
import services.Result
import utils.aliases._

case class CustomerDynamicGroupQualifier(search: CustomerSearch) extends Qualifier {

  val qualifierType: QualifierType = CustomerDynamicGroup

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    search.query(input).map {
      case Xor.Right(count) if count > 0 ⇒ Xor.Right(Unit)
      case _                             ⇒ Xor.Left(SearchFailure.single)
    }
}
