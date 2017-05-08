package models.discount.qualifiers

import cats.implicits._
import failures.DiscountFailures._
import models.discount.{CustomerSearch, DiscountInput}
import utils.aliases._
import utils.apis.Apis
import utils.db._

case class CustomerDynamicGroupQualifier(search: CustomerSearch) extends Qualifier {

  val qualifierType: QualifierType = CustomerDynamicGroup

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    search.query(input).mapEither {
      case Right(count) if count > 0 ⇒ Either.right(Unit)
      case _                         ⇒ Either.left(SearchFailure.single)
    }
}
