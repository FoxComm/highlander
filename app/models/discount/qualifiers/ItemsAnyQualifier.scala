package models.discount.qualifiers

import models.discount.{DiscountInput, ReferenceTuple}
import services.Result
import utils.aliases._

case class ItemsAnyQualifier(references: Seq[ReferenceTuple]) extends Qualifier {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] = reject(input, "Not implemented")
}
