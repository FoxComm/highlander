package models.discount.qualifiers

import cats.implicits._
import failures.DiscountCompilerFailures.QualifierRejectionFailure
import failures.DiscountFailures.SearchFailure
import failures._
import models.discount.{DiscountBase, DiscountInput, ProductSearch}
import utils.ElasticsearchApi.Buckets
import utils.aliases._
import utils.apis.Apis
import utils.db._

trait Qualifier extends DiscountBase {

  val qualifierType: QualifierType

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit]

  def accept()(implicit ec: EC, apis: Apis): Result[Unit] = Result.unit

  def reject(input: DiscountInput, message: String)(implicit ec: EC): Result[Unit] =
    Result.failure(QualifierRejectionFailure(this, input, message))

  def rejectEither(input: DiscountInput, message: String): Either[Failures, Unit] =
    Either.left(QualifierRejectionFailure(this, input, message).single)
}

trait ItemsQualifier extends Qualifier {

  def matchEither(input: DiscountInput)(either: Either[Failures, Buckets]): Either[Failures, Unit] // FIXME: why use matchEithers instead of .map, if *never* do anything with Left? @michalrus

  def checkInner(input: DiscountInput)(
      search: Seq[ProductSearch])(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] = {
    val inAnyOf = search.map(_.query(input).mapEither(matchEither(input)))
    Result.onlySuccessful(inAnyOf.toList).flatMap {
      case xs if xs.nonEmpty ⇒ Result.unit
      case _                 ⇒ Result.failure(SearchFailure)
    }
  }
}
