package services.activity

import scala.language.implicitConversions
import io.circe.syntax._
import models.activity.OpaqueActivity
import utils.snakeCaseName

trait ActivityBase[A] { self: A ⇒

  def typeName: String = snakeCaseName(this)

  def toOpaque[AB <: ActivityBase[AB]]: OpaqueActivity =
    OpaqueActivity(typeName, this.asJson)
}

object ActivityBase {
  implicit def toOpaque[A <: ActivityBase[A]](a: A): OpaqueActivity = a.toOpaque
}
