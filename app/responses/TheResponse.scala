package responses

import responses.BatchMetadata.{BatchFailures, BatchSuccess, SuccessData, FailureData}
import services.Failures
import utils.friendlyClassName

final case class TheResponse[A](
  result     : A,
  pagination : Option[PaginationMetadata] = None,
  sorting    : Option[SortingMetadata] = None,
  alerts     : Option[List[String]] = None,
  errors     : Option[List[String]] = None,
  warnings   : Option[List[String]] = None,
  batch      : Option[BatchMetadata] = None)

object TheResponse {

  def build[A](value       : A,
               pagination  : Option[PaginationMetadata] = None,
               sorting     : Option[SortingMetadata] = None,
               alerts      : Option[Failures] = None,
               errors      : Option[Failures] = None,
               warnings    : Option[Failures] = None,
               batch       : Option[BatchMetadata] = None): TheResponse[A] =
    TheResponse(result     = value,
                pagination = pagination,
                sorting    = sorting,
                alerts     = alerts.map(_.flatten),
                errors     = errors.map(_.flatten),
                warnings   = warnings.map(_.flatten),
                batch      = batch)
}

final case class PaginationMetadata(
  from      : Option[Int] = None,
  size      : Option[Int] = None,
  pageNo    : Option[Int] = None,
  total     : Option[Int] = None)

final case class SortingMetadata(sortBy: Option[String] = None)

final case class BatchMetadata(success: BatchSuccess, failures: BatchFailures) {

  def flatten: Option[List[String]] = {
    val errors = failures.values.flatMap(_.values)
    if (errors.nonEmpty) Some(errors.toList) else None
  }
}

object BatchMetadata {
  type EntityType   = String
  type SuccessData  = Seq[String]
  type FailureData  = Map[String, String]

  type BatchSuccess   = Map[EntityType, SuccessData]
  type BatchFailures  = Map[EntityType, FailureData]

  def apply(input: BatchMetadataSource): BatchMetadata = buildInner(Seq(input))
  def apply(input: Seq[BatchMetadataSource]): BatchMetadata = buildInner(input)

  private def buildInner(input: Seq[BatchMetadataSource]): BatchMetadata = {
    val success = input.foldLeft(Map[EntityType, SuccessData]()) { case (acc, src) ⇒
      acc.updated(src.className, src.success)
    }

    val failures = input.foldLeft(Map[EntityType, FailureData]()) { case (acc, src) ⇒
      acc.updated(src.className, src.failures)
    }

    BatchMetadata(success = success, failures = failures)
  }

  def flattenErrors(input: FailureData): Option[List[String]] = {
    val errors = input.values.toList
    if (errors.nonEmpty) Some(errors) else None
  }
}

final case class BatchMetadataSource(className: String, success: SuccessData, failures: FailureData)

object BatchMetadataSource {
  def apply[A](model: A, success: SuccessData, failures: FailureData): BatchMetadataSource = {
    BatchMetadataSource(friendlyClassName(model), success, failures)
  }
}