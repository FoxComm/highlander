package responses

import cats.data.Xor
import services.{Result, Failure, Failures}
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
  total     : Option[Int] = None) extends CheckDefined

final case class ResponseSortingMetadata(sortBy: Option[String] = None) extends CheckDefined

final case class ResponseWithFailuresAndMetadata[A <: AnyRef](
  result    : A, 
  errors    : Option[Seq[String]]             = None,
  pagination: Option[ResponsePagingMetadata]  = None,
  sorting   : Option[ResponseSortingMetadata] = None) {

  import ResponseWithFailuresAndMetadata._

  def withFailures(failures: Option[Failures]): ResponseWithFailuresAndMetadata[A] =
    this.copy(errors = failures.map(failuresToSeqStrings))

  def addFailures(failures: Option[Failures]): ResponseWithFailuresAndMetadata[A] =
    this.copy(errors = failures.map(f ⇒ errors.getOrElse(Seq.empty) ++ failuresToSeqStrings(f)).orElse(errors))

  def addFailures(failures: Seq[Failure]): ResponseWithFailuresAndMetadata[A] =
    if (failures.isEmpty) this else this.addFailures(Some(Failures(failures: _*)))
}

object ResponseWithFailuresAndMetadata {

  private def failuresToSeqStrings(failures: Failures): Seq[String] = failures.toList.flatMap(_.description)

  def fromOption[A <: AnyRef](result: A, failures: Option[Failures]): ResponseWithFailuresAndMetadata[A] = {
    val errors = failures.map(failuresToSeqStrings)
    ResponseWithFailuresAndMetadata(
      result = result,
      errors = errors)
  }

  def fromFailures[A <: AnyRef](result: A, failures: Failures): ResponseWithFailuresAndMetadata[A] =
    ResponseWithFailuresAndMetadata.fromOption(result, Some(failures))

  def fromFailureList[A <: AnyRef](result: A, failures: Seq[Failure]): ResponseWithFailuresAndMetadata[A] =
    if (failures.isEmpty) noFailures(result) else ResponseWithFailuresAndMetadata.fromFailures(result, Failures(failures: _*))

  def noFailures[A <: AnyRef](result: A): ResponseWithFailuresAndMetadata[A] =
    ResponseWithFailuresAndMetadata.fromOption(result, None)

  def withMetadata[A <: AnyRef](result : A,
    metadata: Option[ResponseMetadata] = None): ResponseWithFailuresAndMetadata[A] = {
    ResponseWithFailuresAndMetadata(
      result     = result,
      sorting    = metadata.flatMap ( m ⇒ ResponseSortingMetadata(sortBy = m.sortBy).ifDefined ),
      pagination = metadata.flatMap { m ⇒
        ResponsePagingMetadata(
          from = m.from,
          size = m.size,
          pageNo = m.pageNo,
          total = m.total).ifDefined
      }
    )
  }

  def xorFromXor[A <: AnyRef](result: Failures Xor A, addFailures: Seq[Failure] = Seq.empty,
    metadata: Option[ResponseMetadata] = None): Failures Xor ResponseWithFailuresAndMetadata[A] = result.bimap (
    errors ⇒ if (addFailures.isEmpty) errors else Failures(errors.toList ++ addFailures: _*),
    res    ⇒ ResponseWithFailuresAndMetadata.withMetadata(res, metadata).addFailures(addFailures)
  )

  type BulkOrderUpdateResponse = ResponseWithFailuresAndMetadata[Seq[AllOrders.Root]]
}

