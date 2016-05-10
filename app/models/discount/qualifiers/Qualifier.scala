package models.discount.qualifiers

import failures.DiscountCompilerFailures.QualifierRejectionFailure
import models.discount.DiscountInput
import services.Result
import utils.aliases._

trait Qualifier {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit]

  def accept()(implicit ec: EC, es: ES): Result[Unit] = Result.unit

  def reject(input: DiscountInput, message: String): Result[Unit] =
    Result.failure(QualifierRejectionFailure(this, input.order.refNum, message))
}
