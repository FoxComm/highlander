package payloads

import models.image._
import models.objects._
import org.json4s.JsonAST.{JNothing, JObject}
import org.json4s.JsonDSL._
import utils.aliases._

object ImagePayloads {

  type Images = Option[Seq[ImagePayload]]

  val imageShadow = ("type" → "images") ~ ("ref" → "images")
  val nameShadow  = ("type" → "string") ~ ("ref" → "name")

  case class ImagePayload(src: String, title: Option[String] = None, alt: Option[String] = None) {
    def toJson: Json = ("src" -> src) ~ ("title" -> title) ~ ("alt" -> alt)
  }

  case class CreateAlbumPayload(name: String, images: Images = None) {
    def objectForm: ObjectForm = {
      val imageJson = images.map(_.map(_.toJson))
      val json      = ("name" → name) ~ ("images" → imageJson)
      ObjectForm(kind = Album.kind, attributes = json)
    }

    def objectShadow: ObjectShadow = {
      val imageJson = "images" → images.map(_ ⇒ imageShadow)
      val nameJson  = "name"   → nameShadow
      ObjectShadow(attributes = nameJson ~ imageJson)
    }
  }

  case class UpdateAlbumPayload(name: Option[String] = None, images: Images = None) {
    val objectForm: ObjectForm = {
      def imageJson(img: Seq[ImagePayload]): JObject = "images" → img.map(_.toJson)
      def nameJson(name: String): JObject            = "name"   → name

      val json = (name, images) match {
        case (Some(n), Some(img)) ⇒ imageJson(img) ~ nameJson(n)
        case (Some(n), _)         ⇒ nameJson(n)
        case (_, Some(img))       ⇒ imageJson(img)
        case _                    ⇒ JNothing
        // TODO: validate payload to prohibit both `None`s
      }
      ObjectForm(kind = Album.kind, attributes = json)
    }

    def objectShadow: ObjectShadow = {
      val imageJson = images.fold(JObject())(img ⇒ "images" → img.map(_.toJson))
      val nameJson  = name.fold(JObject())(n ⇒ "name"       → n)
      ObjectShadow(attributes = nameJson ~ imageJson)
    }
  }
}
