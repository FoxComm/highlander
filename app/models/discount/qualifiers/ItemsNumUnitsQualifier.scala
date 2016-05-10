package models.discount.qualifiers

import models.discount.{DiscountInput, ReferenceTuple}
import services._
import utils.aliases._

case class ItemsNumUnitsQualifier(numUnits: Int, reference: Seq[ReferenceTuple]) extends Qualifier {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit] = reject(input, "Not implemented")
}
