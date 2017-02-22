package models.discount.qualifiers

import cats.implicits._
import models.discount.DiscountInput
import services.Result
import utils.aliases._

case class AndQualifier(qualifiers: Seq[Qualifier]) extends Qualifier {

  val qualifierType: QualifierType = And

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] =
    Result.sequenceJoiningFailures(qualifiers.map(_.check(input)).toList).map(_ ⇒ ())

}
