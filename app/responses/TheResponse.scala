package responses

import services.Failures

final case class TheResponse[A](
  result     : A,
  pagination : Option[PaginationMetadata] = None,
  sorting    : Option[SortingMetadata] = None,
  alerts     : Option[List[String]] = None,
  errors     : Option[List[String]] = None,
  warnings   : Option[List[String]] = None)

object TheResponse {

  def build[A](value       : A,
               pagination  : Option[PaginationMetadata] = None,
               sorting     : Option[SortingMetadata] = None,
               alerts      : Option[Failures] = None,
               errors      : Option[Failures] = None,
               warnings    : Option[Failures] = None): TheResponse[A] =
    TheResponse(result     = value,
                pagination = pagination,
                sorting    = sorting,
                alerts     = alerts.map(_.flatten),
                errors     = errors.map(_.flatten),
                warnings   = warnings.map(_.flatten))
}

final case class PaginationMetadata(
  from      : Option[Int] = None,
  size      : Option[Int] = None,
  pageNo    : Option[Int] = None,
  total     : Option[Int] = None)

final case class SortingMetadata(sortBy: Option[String] = None)
