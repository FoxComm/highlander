package models.discount.qualifiers

import scala.concurrent.Future

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

  def reject(input: DiscountInput, message: String): Result[Unit] =
    Result.failure(QualifierRejectionFailure(this, input, message))

  def rejectXor(input: DiscountInput, message: String): Xor[Failures, Unit] =
    Xor.Left(QualifierRejectionFailure(this, input, message).single)
}

trait ItemsQualifier extends Qualifier {

  def matchXor(input: DiscountInput)(xor: Failures Xor Buckets): Failures Xor Unit

  def checkInner(input: DiscountInput)(
      search: Seq[ProductSearch])(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] = {
    val inAnyOf = search.map(_.query(input).map(matchXor(input)))

    Future
      .sequence(inAnyOf)
      .flatMap(xorSequence ⇒
            xorSequence.find(_.isRight) match {
          case Some(x) ⇒ Result.unit
          case None    ⇒ Result.failure(SearchFailure)
      })
  }
}
