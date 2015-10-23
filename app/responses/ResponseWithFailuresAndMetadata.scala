package responses

import services.{Failure, Failures}
import utils.Slick.implicits.ResponseMetadata

sealed trait CheckDefined { self: Product ⇒

  def isDefined: Boolean = this.productIterator.exists {
    case None ⇒ false
    case _    ⇒ true
  }

  def ifDefined: Option[this.type] = if(isDefined) Some(this) else None
}

final case class ResponsePagingMetadata(
  from      : Option[Int] = None,
  size      : Option[Int] = None,
  pageNo    : Option[Int] = None,
  totalPages: Option[Int] = None) extends CheckDefined

final case class ResponseSortingMetadata(sortBy: Option[String] = None) extends CheckDefined

final case class ResponseWithFailuresAndMetadata[A <: AnyRef](
  result    : A, 
  errors    : Option[Seq[String]]                 = None,
  pagination: Option[ResponsePagingMetadata]  = None,
  sorting   : Option[ResponseSortingMetadata] = None)

object ResponseWithFailuresAndMetadata {

  def fromOption[A <: AnyRef](result: A, failures: Option[Failures]): ResponseWithFailuresAndMetadata[A] = {
    val list = failures.map(_.toList.flatMap(_.description))
    ResponseWithFailuresAndMetadata(
      result = result,
      errors = list)
  }

  def fromFailures[A <: AnyRef](result: A, failures: Failures): ResponseWithFailuresAndMetadata[A] =
    ResponseWithFailuresAndMetadata.fromOption(result, Some(failures))

  def fromFailureList[A <: AnyRef](result: A, failures: Seq[Failure]): ResponseWithFailuresAndMetadata[A] =
    if (failures.isEmpty) noFailures(result) else ResponseWithFailuresAndMetadata.fromFailures(result, Failures(failures: _*))

  def noFailures[A <: AnyRef](result: A): ResponseWithFailuresAndMetadata[A] =
    ResponseWithFailuresAndMetadata.fromOption(result, None)

  def withMetadata[A <: AnyRef](result : A, metadata: ResponseMetadata): ResponseWithFailuresAndMetadata[A] = {
    ResponseWithFailuresAndMetadata(
      result = result,
      sorting = ResponseSortingMetadata(sortBy = metadata.sortBy).ifDefined,
      pagination = ResponsePagingMetadata(
        from = metadata.from,
        size = metadata.size,
        pageNo = metadata.pageNo,
        totalPages = metadata.totalPages).ifDefined
    )
  }

  type BulkOrderUpdateResponse = ResponseWithFailuresAndMetadata[Seq[AllOrders.Root]]
}

