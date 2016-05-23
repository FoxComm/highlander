package payloads

import models.image._
import models.objects._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._

object ImagePayloads {

  case class ImagePayload(src: String, title: Option[String] = None, alt: Option[String] = None) {
    def toJson: JValue = ("src" -> src) ~ ("title" -> title) ~ ("alt" -> alt)
  }

  case class AlbumPayload(name: String, images: Option[Seq[ImagePayload]] = None) {
    def objectForm: ObjectForm = {
      val imageJson = images.map(_.map(_.toJson))
      val json = ("name" -> name) ~ ("images" -> imageJson)
      ObjectForm(kind = Album.kind, attributes = json)
    }

    def objectShadow: ObjectShadow = {
      val imageJson = images.map(_ ⇒ ("type" -> "images") ~ ("ref" -> "images"))
      val json = ("name" -> ("type" -> "string") ~ ("ref" -> "name")) ~ ("images" -> imageJson)
      ObjectShadow(attributes = json)
    }
  }
}
