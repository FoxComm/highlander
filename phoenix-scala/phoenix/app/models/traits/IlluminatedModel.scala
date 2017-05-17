package models.traits

import cats.implicits._
import failures.{Failure, Failures}
import java.time.Instant
import utils.JsonFormatters
import utils.aliases.Json

trait IlluminatedModel[T] {

  implicit val formats = JsonFormatters.phoenixFormats

  def archivedAt: Option[Instant]

  def attributes: Json

  def inactiveError: Failure

  def isActive: Boolean =
    archivedAt.isEmpty && {
      val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
      val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
      val now        = Instant.now

      (activeFrom, activeTo) match {
        case (Some(from), Some(to)) ⇒ from.isBefore(now) && to.isAfter(now)
        case (Some(from), None)     ⇒ from.isBefore(now)
        case _                      ⇒ false
      }
    }

  def mustBeActive: Either[Failures, IlluminatedModel[T]] =
    if (isActive) Either.right(this) else Either.left(inactiveError.single)

}
