package phoenix.models.discount.qualifiers

import cats.implicits._
import core.db._
import core.failures._
import phoenix.failures.DiscountCompilerFailures.QualifierRejectionFailure
import phoenix.failures.DiscountFailures.SearchFailure
import phoenix.models.discount.{DiscountBase, DiscountInput, ProductSearch}
import phoenix.utils.ElasticsearchApi.Buckets
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis

trait Qualifier extends DiscountBase {

  val qualifierType: QualifierType

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit]

  def accept()(implicit ec: EC, apis: Apis): Result[Unit] = ().pure[Result]

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
      case xs if xs.nonEmpty ⇒ ().pure[Result]
      case _                 ⇒ Result.failure(SearchFailure)
    }
  }
}
