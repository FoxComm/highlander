package responses

import java.time.Instant

import cats.implicits._
import models.image._
import models.objects._
import org.json4s.JsonAST._
import utils.{IlluminateAlgorithm, JsonFormatters}

object ImageResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  object AlbumResponse {
    case class Root(
        id: Int, name: String, images: Seq[Image], createdAt: Instant, updatedAt: Instant)
        extends ResponseItem

    def build(album: FullObject[Album], images: Seq[Image]): Root = {
      val model       = album.model
      val formAttrs   = album.form.attributes
      val shadowAttrs = album.shadow.attributes

      val name = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]

      Root(id = album.form.id,
           name = name,
           images = images,
           createdAt = model.createdAt,
           updatedAt = model.updatedAt)
    }
  }
}
