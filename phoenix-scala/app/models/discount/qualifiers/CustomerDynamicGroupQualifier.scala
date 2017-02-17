package models.discount.qualifiers

import cats._
import cats.implicits._
import cats.data._
import failures.DiscountFailures._
import models.discount.{CustomerSearch, DiscountInput}
import services.Result
import utils.aliases._

case class CustomerDynamicGroupQualifier(search: CustomerSearch) extends Qualifier {

  val qualifierType: QualifierType = CustomerDynamicGroup

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    search.query(input).mapXor {
      case Xor.Right(count) if count > 0 ⇒ Xor.Right(Unit)
      case _                             ⇒ Xor.Left(SearchFailure.single)
    }
}
