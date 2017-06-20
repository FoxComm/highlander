package phoenix.models.discount.qualifiers

import cats.implicits._
import core.db._
import phoenix.failures.DiscountFailures._
import phoenix.models.discount.{CustomerSearch, DiscountInput}
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class CustomerDynamicGroupQualifier(search: CustomerSearch) extends Qualifier {

  val qualifierType: QualifierType = CustomerDynamicGroup

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    search.query(input).mapEither {
      case Right(count) if count > 0 ⇒ Either.right(Unit)
      case _                         ⇒ Either.left(SearchFailure.single)
    }
}
