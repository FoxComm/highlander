package responses

import cats.{Functor, Monad}
import failures._
import responses.BatchMetadata._
import services.CartValidatorResponse
import utils.friendlyClassName

case class TheResponse[A](result: A,
                          alerts: Option[List[String]] = None,
                          errors: Option[List[String]] = None,
                          warnings: Option[List[String]] = None,
                          batch: Option[BatchMetadata] = None)

object TheResponse {

  def build[A](value: A,
               alerts: Option[Failures] = None,
               errors: Option[Failures] = None,
               warnings: Option[Failures] = None,
               batch: Option[BatchMetadata] = None): TheResponse[A] =
    TheResponse(result = value,
                alerts = alerts.map(_.flatten),
                errors = errors.map(_.flatten),
                warnings = warnings.map(_.flatten),
                batch = batch)

  def validated[A](value: A, validatorResponse: CartValidatorResponse): TheResponse[A] =
    TheResponse(result = value,
                alerts = validatorResponse.alerts.map(_.flatten),
                warnings = validatorResponse.warnings.map(_.flatten))

  implicit val theResponseFunctor = new Functor[TheResponse] with Monad[TheResponse] {
    override def pure[A](x: A): TheResponse[A] = TheResponse(x)

    // FIXME: this monstrosity below suggests that stuff could probably be encoded better (as in «more composable») @michalrus
    override def flatMap[A, B](fa: TheResponse[A])(f: (A) ⇒ TheResponse[B]): TheResponse[B] = {
      val fb = f(fa.result)
      def combineOL[C](xs: Option[List[C]], ys: Option[List[C]]): Option[List[C]] =
        (xs.toList.flatten ::: ys.toList.flatten) match {
          case Nil ⇒ None
          case xs  ⇒ Some(xs)
        }
      def combineBatch(a: BatchMetadata, b: BatchMetadata): BatchMetadata =
        BatchMetadata(success = a.success ++ b.success, failures = a.failures ++ b.failures)
      TheResponse(
          result = fb.result,
          alerts = combineOL(fa.alerts, fb.alerts),
          errors = combineOL(fa.errors, fb.errors),
          warnings = combineOL(fa.warnings, fb.warnings),
          batch = (fa.batch, fb.batch) match {
            case (Some(a), Some(b)) ⇒ Some(combineBatch(a, b))
            case (oa, None)         ⇒ oa
            case (None, ob)         ⇒ ob
            case _                  ⇒ None
          }
      )
    }

    override def tailRecM[A, B](a: A)(f: (A) ⇒ TheResponse[Either[A, B]]): TheResponse[B] =
      defaultTailRecM(a)(f)
  }
}

case class BatchMetadata(success: BatchSuccess, failures: BatchFailures) {

  def flatten: Option[List[String]] = {
    val errors = failures.values.flatMap(_.values)
    if (errors.nonEmpty) Some(errors.toList) else None
  }
}

object BatchMetadata {
  type EntityType  = String
  type SuccessData = Seq[String]
  type FailureData = Map[String, String]

  type BatchSuccess  = Map[EntityType, SuccessData]
  type BatchFailures = Map[EntityType, FailureData]

  def apply(input: BatchMetadataSource): BatchMetadata      = buildInner(Seq(input))
  def apply(input: Seq[BatchMetadataSource]): BatchMetadata = buildInner(input)

  private def buildInner(input: Seq[BatchMetadataSource]): BatchMetadata = {
    val success = input.foldLeft(Map[EntityType, SuccessData]()) {
      case (acc, src) ⇒
        acc.updated(src.className, src.success)
    }

    val failures = input.foldLeft(Map[EntityType, FailureData]()) {
      case (acc, src) ⇒
        acc.updated(src.className, src.failures)
    }

    BatchMetadata(success = success, failures = failures)
  }

  def flattenErrors(input: FailureData): Option[List[String]] = {
    val errors = input.values.toList
    if (errors.nonEmpty) Some(errors) else None
  }
}

case class BatchMetadataSource(className: String, success: SuccessData, failures: FailureData)

object BatchMetadataSource {
  def apply[A](model: A, success: SuccessData, failures: FailureData): BatchMetadataSource = {
    BatchMetadataSource(friendlyClassName(model), success, failures)
  }
}
