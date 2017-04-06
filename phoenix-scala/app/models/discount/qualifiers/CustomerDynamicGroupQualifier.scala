package models.discount.qualifiers

import cats.data._
import cats.implicits._
import failures.DiscountFailures._
import models.discount.{CustomerSearch, DiscountInput}
import utils.aliases._
import utils.apis.Apis
import utils.db._

case class CustomerDynamicGroupQualifier(search: CustomerSearch) extends Qualifier {

  val qualifierType: QualifierType = CustomerDynamicGroup

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    search.query(input).mapXor {
      case Xor.Right(count) if count > 0 ⇒ Xor.Right(Unit)
      case _                             ⇒ Xor.Left(SearchFailure.single)
    }
}
