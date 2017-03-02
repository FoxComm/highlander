package responses

import failures._
import responses.BatchMetadata._
import services.CartValidatorResponse
import utils.friendlyClassName

case class TheResponse[A <: AnyRef](result: A,
                                    alerts: Option[List[String]] = None,
                                    errors: Option[List[String]] = None,
                                    warnings: Option[List[String]] = None,
                                    batch: Option[BatchMetadata] = None)

object TheResponse {

  def build[A <: AnyRef](value: A,
                         alerts: Option[Failures] = None,
                         errors: Option[Failures] = None,
                         warnings: Option[Failures] = None,
                         batch: Option[BatchMetadata] = None): TheResponse[A] =
    TheResponse(result = value,
                alerts = alerts.map(_.flatten),
                errors = errors.map(_.flatten),
                warnings = warnings.map(_.flatten),
                batch = batch)

  def validated[A <: AnyRef](value: A, validatorResponse: CartValidatorResponse): TheResponse[A] =
    TheResponse(result = value,
                alerts = validatorResponse.alerts.map(_.flatten),
                warnings = validatorResponse.warnings.map(_.flatten))
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

  private def buildInner(input: Seq[BatchMetadataSource]): BatchMetadata =
    BatchMetadata(success = input.map(src ⇒ src.className  → src.success).toMap,
                  failures = input.map(src ⇒ src.className → src.failures).toMap)

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
