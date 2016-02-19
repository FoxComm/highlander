package responses

import responses.BatchMetadata.{BatchFailures, BatchSuccess}
import services.Failures

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

  def flatten(): Option[List[String]] = {
    val errors = failures.values.flatMap(_.values)
    if (errors.nonEmpty) {
      Some(errors.toList)
    } else {
      None
    }
  }
}

object BatchMetadata {
  type ClassName      = String
  type SuccessIds     = Seq[String]
  type ErrorMessages  = Map[String, String]

  type RawMetadata    = (ClassName, SuccessIds, ErrorMessages)
  type BatchSuccess   = Map[String, SuccessIds]
  type BatchFailures  = Map[String, ErrorMessages]

  def build(input: List[RawMetadata]): BatchMetadata = {
    val success = input.foldLeft(Map[String, Seq[String]]()) { case (acc, (typeName, identifiers, _)) ⇒
      acc.updated(typeName, identifiers)
    }

    val failures = input.foldLeft(Map[String, Map[String, String]]()) { case (acc, (typeName, _, errors)) ⇒
      acc.updated(typeName, errors)
    }

    BatchMetadata(success = success, failures = failures)
  }
}