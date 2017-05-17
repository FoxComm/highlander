package models.discount.qualifiers

import cats.implicits._
import models.discount.DiscountInput
import utils.aliases._
import utils.apis.Apis
import utils.db._

case class AndQualifier(qualifiers: Seq[Qualifier]) extends Qualifier {

  val qualifierType: QualifierType = And

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    Result.seqCollectFailures(qualifiers.map(_.check(input)).toList).map(_ ⇒ ())

}
