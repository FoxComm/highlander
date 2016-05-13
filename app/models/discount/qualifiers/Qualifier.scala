package models.discount.qualifiers

import cats.data.Xor
import failures._
import failures.DiscountCompilerFailures.QualifierRejectionFailure
import models.discount.{DiscountBase, DiscountInput}
import services.Result
import utils.aliases._

trait Qualifier extends DiscountBase {

  def check(input: DiscountInput)(implicit ec: EC, es: ES): Result[Unit]

  def accept()(implicit ec: EC, es: ES): Result[Unit] = Result.unit

  def reject(input: DiscountInput, message: String): Result[Unit] =
    Result.failure(QualifierRejectionFailure(this, input, message))

  def rejectXor(input: DiscountInput, message: String): Xor[Failures, Unit] =
    Xor.Left(QualifierRejectionFailure(this, input, message).single)
}
