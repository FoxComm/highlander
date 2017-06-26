package phoenix.models.discount.qualifiers

import cats.implicits._
import core.db._
import phoenix.models.discount.DiscountInput
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

case class AndQualifier(qualifiers: Seq[Qualifier]) extends Qualifier {

  val qualifierType: QualifierType = And

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    Result.seqCollectFailures(qualifiers.map(_.check(input)).toList).map(_ ⇒ ())

}
