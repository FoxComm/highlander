package testutils

import cats.implicits._
import io.circe._
import io.circe.syntax._
import java.time.Instant
import payloads.ImagePayloads.{AlbumPayload, ImagePayload}
import utils.aliases._

object PayloadHelpers {
  case class ShadowValue(v: Json, t: String)

  implicit class ShadowExtString(val s: String) extends AnyVal {
    def richText: ShadowValue =
      ShadowValue(t = "richText", v = Json.fromString(s))
  }

  implicit class ShadowExtJson(val o: Json) extends AnyVal {
    def asShadowVal(t: String): ShadowValue =
      ShadowValue(t = t, v = o)
  }

  def tv[T: Encoder](v: T, t: String = "string"): Json =
    Json.obj("t" → Json.fromString(t), "v" → v.asJson)

  def usdPrice(price: Int): Json =
    tv(Json.obj("currency" → Json.fromString("USD"), "value" → Json.fromInt(price)), "price")

  implicit class AttributesJsonifyValues(val attrs: Map[String, Any]) extends AnyVal {
    def asShadow: Map[String, Json] =
      attrs.mapValues {
        case v: String         ⇒ tv(v)
        case i: Int            ⇒ tv(i, t = "number")
        case d: Instant        ⇒ tv(d, t = "datetime")
        case ShadowValue(v, t) ⇒ tv(v, t = t)
        case v: Json           ⇒ v
        case e                 ⇒ throw new IllegalArgumentException(s"Can't find valid shadow type for value $e")

      }
  }

  val imageSrc: String = "http://lorempixel/test.png"
  val someAlbums: Option[Seq[AlbumPayload]] = Seq(
      AlbumPayload(name = "Default".some, images = Seq(ImagePayload(src = imageSrc)).some)).some
}
