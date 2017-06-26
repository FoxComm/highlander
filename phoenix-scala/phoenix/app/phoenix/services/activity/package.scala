package phoenix.services.activity

import scala.language.implicitConversions
import core.utils.snakeCaseName
import org.json4s.Extraction
import phoenix.models.activity.OpaqueActivity
import phoenix.utils.JsonFormatters

trait ActivityBase[A] { self: A ⇒

  def typeName: String = snakeCaseName(this)

  implicit val formats = JsonFormatters.phoenixFormats

  def toOpaque: OpaqueActivity =
    OpaqueActivity(typeName, Extraction.decompose(this))
}

object ActivityBase {
  implicit def toOpaque[A <: ActivityBase[A]](a: A): OpaqueActivity = a.toOpaque
}
