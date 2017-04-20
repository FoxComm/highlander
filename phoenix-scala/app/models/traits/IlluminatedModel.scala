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

  protected def inactiveError: Failure

  def mustBeActive: Either[Failures, IlluminatedModel[T]] = {
    if (archivedAt.isDefined) {
      Either.left(inactiveError.single)
    } else {
      val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
      val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
      val now        = Instant.now

      (activeFrom, activeTo) match {
        case (Some(from), Some(to)) ⇒
          if (from.isBefore(now) && to.isAfter(now)) Either.right(this)
          else Either.left(inactiveError.single)
        case (Some(from), None) ⇒
          if (from.isBefore(now)) Either.right(this)
          else Either.left(inactiveError.single)
        case _ ⇒
          Either.left(inactiveError.single)
      }
    }
  }

}
