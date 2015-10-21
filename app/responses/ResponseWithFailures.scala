package responses

import services.{Failure, Failures}

case class ResponseWithFailures[A <: AnyRef](result: A, errors: Option[Seq[String]])

object ResponseWithFailures {

  def fromOption[A <: AnyRef](result: A, failures: Option[Failures]): ResponseWithFailures[A] = {
    val list = failures.map(_.toList.flatMap(_.description))
    ResponseWithFailures(result, list)
  }

  def fromFailures[A <: AnyRef](result: A, failures: Failures): ResponseWithFailures[A] =
    ResponseWithFailures.fromOption(result, Some(failures))

  def fromFailureList[A <: AnyRef](result: A, failures: Seq[Failure]): ResponseWithFailures[A] =
    if (failures.isEmpty) noFailures(result) else ResponseWithFailures.fromFailures(result, Failures(failures: _*))

  def noFailures[A <: AnyRef](result: A): ResponseWithFailures[A] =
    ResponseWithFailures.fromOption(result, None)

  type BulkOrderUpdateResponse = ResponseWithFailures[Seq[AllOrders.Root]]
}
