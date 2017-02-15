package models.discount.qualifiers

import scala.concurrent.Future

import cats.implicits._
import cats.data.Xor
import failures.DiscountCompilerFailures.QualifierRejectionFailure
import failures.DiscountFailures.SearchFailure
import failures._
import models.discount.{DiscountBase, DiscountInput, ProductSearch}
import services.Result
import utils.ElasticsearchApi.Buckets
import utils.aliases._

trait Qualifier extends DiscountBase {

  val qualifierType: QualifierType

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit]

  def accept()(implicit ec: EC, es: ES): Result[Unit] = Result.unit

  def reject(input: DiscountInput, message: String)(implicit ec: EC): Result[Unit] =
    Result.failure(QualifierRejectionFailure(this, input, message))

  def rejectXor(input: DiscountInput, message: String): Xor[Failures, Unit] =
    Xor.Left(QualifierRejectionFailure(this, input, message).single)
}

trait ItemsQualifier extends Qualifier {

  def matchXor(input: DiscountInput)(xor: Failures Xor Buckets): Failures Xor Unit // FIXME: why use matchXor instead of .map, if *never* do anything with Left? @michalrus

  def checkInner(input: DiscountInput)(
      search: Seq[ProductSearch])(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] = {
    val inAnyOf = search.map(_.query(input).mapXor(matchXor(input)))
    Result.onlySuccessful(inAnyOf).flatMap {
      case xs if xs.nonEmpty ⇒ Result.unit
      case _                 ⇒ Result.failure(SearchFailure)
    }
  }
}
