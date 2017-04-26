package models.traits

import cats.implicits._
import failures.{Failure, Failures}
import java.time.Instant
import utils.aliases.Json
import utils.json.codecs._

trait IlluminatedModel[T] {

  def archivedAt: Option[Instant]

  def attributes: Json

  protected def inactiveError: Failure

  def mustBeActive: Either[Failures, IlluminatedModel[T]] = {
    if (archivedAt.isDefined) {
      Either.left(inactiveError.single)
    } else {
      val attrsC     = attributes.hcursor
      val activeFrom = attrsC.downField("activeFrom").downField("v").as[Instant].toOption
      val activeTo   = attrsC.downField("activeTo").downField("v").as[Instant].toOption
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
