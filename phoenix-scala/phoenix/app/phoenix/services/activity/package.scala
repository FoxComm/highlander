package phoenix.services.activity

import org.json4s.Extraction
import phoenix.models.activity.OpaqueActivity
import phoenix.utils.JsonFormatters
import utils.snakeCaseName

import scala.language.implicitConversions

trait ActivityBase[A] { self: A ⇒

  def typeName: String = snakeCaseName(this)

  implicit val formats = JsonFormatters.phoenixFormats

  def toOpaque[AB <: ActivityBase[AB]]: OpaqueActivity =
    OpaqueActivity(typeName, Extraction.decompose(this))
}

object ActivityBase {
  implicit def toOpaque[A <: ActivityBase[A]](a: A): OpaqueActivity = a.toOpaque
}
