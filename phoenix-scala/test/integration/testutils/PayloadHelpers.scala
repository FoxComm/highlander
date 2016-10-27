package testutils

import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import utils.aliases._
import java.time.Instant
import utils.JsonFormatters
import org.json4s.Extraction.decompose

object PayloadHelpers {
  implicit val formats = JsonFormatters.phoenixFormats

  case class ShadowValue(v: Any, t: String)

  implicit class ShadowExtString(val s: String) extends AnyVal {
    def richText: ShadowValue =
      ShadowValue(t = "richText", v = s)
  }

  implicit class ShadowExtJson(val o: JValue) extends AnyVal {
    def asShadowVal(t: String): ShadowValue =
      ShadowValue(t = t, v = o)
  }

  def tv(v: Any, t: String = "string"): JObject =
    v match {
      case o: JValue ⇒ ("t" → t) ~ ("v" → o)
      case _         ⇒ ("t" → t) ~ ("v" → decompose(v))
    }

  implicit class AttributesJsonifyValues(val attrs: Map[String, Any]) extends AnyVal {
    def asShadow: Map[String, Json] =
      attrs.mapValues {
        case v: String         ⇒ ("t" → "string") ~ ("v" → v)
        case i: Int            ⇒ ("t" → "number") ~ ("v" → i)
        case d: Instant        ⇒ ("t" → "datetime") ~ ("v" → d.toString)
        case ShadowValue(v, t) ⇒ ("t" → t) ~ ("v" → decompose(v))
        case v: Json           ⇒ v
        case e                 ⇒ throw new IllegalArgumentException(s"Can't find valid shadow type for value $e")

      }
  }

}
