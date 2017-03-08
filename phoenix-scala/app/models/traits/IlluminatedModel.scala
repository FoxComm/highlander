package models.traits

import java.time.Instant

import cats.data.Xor
import cats.data.Xor.{Left, right}
import failures.{Failure, Failures}
import utils.JsonFormatters
import utils.aliases.Json

trait IlluminatedModel[T] {

  implicit val formats = JsonFormatters.phoenixFormats

  def archivedAt: Option[Instant]

  def attributes: Json

  protected def inactiveError: Failure

  def mustBeActive: Failures Xor IlluminatedModel[T] = {
    if (archivedAt.isDefined) {
      Left(inactiveError.single)
    } else {
      val activeFrom = (attributes \ "activeFrom" \ "v").extractOpt[Instant]
      val activeTo   = (attributes \ "activeTo" \ "v").extractOpt[Instant]
      val now        = Instant.now

      (activeFrom, activeTo) match {
        case (Some(from), Some(to)) ⇒
          if (from.isBefore(now) && to.isAfter(now)) right(this)
          else Left(inactiveError.single)
        case (Some(from), None) ⇒
          if (from.isBefore(now)) right(this)
          else Left(inactiveError.single)
        case _ ⇒
          Left(inactiveError.single)
      }
    }
  }

}
