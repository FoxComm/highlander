package services.activity

import models.activity.OpaqueActivity
import org.json4s.{Extraction, DefaultFormats}

import scala.language.implicitConversions

import utils.{JsonFormatters, snakeCaseName}

trait ActivityBase[A] { self: A ⇒

  def typeName: String = snakeCaseName(this)

  implicit val formats = JsonFormatters.phoenixFormats

  def toOpaque[AB <: ActivityBase[AB]]: OpaqueActivity =
    OpaqueActivity(typeName, Extraction.decompose(this))
}

object ActivityBase {
  implicit def toOpaque[A <: ActivityBase[A]](a: A): OpaqueActivity = a.toOpaque
}
