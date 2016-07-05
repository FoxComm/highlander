package responses

import java.time.Instant

import models.image._
import models.objects._
import responses.ImageResponses.IlluminatedImageResponse
import utils.{JsonFormatters, IlluminateAlgorithm}

object AlbumResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  object AlbumResponse {
    case class Root(id: Int,
                    name: String,
                    images: Seq[IlluminatedImageResponse.Root],
                    position: Option[Int],
                    createdAt: Instant,
                    updatedAt: Instant)
        extends ResponseItem

    def build(album: FullObject[Album], images: Seq[FullObject[Image]]): Root = {
      val model       = album.model
      val formAttrs   = album.form.attributes
      val shadowAttrs = album.shadow.attributes

      val name     = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]
      val position = IlluminateAlgorithm.get("position", formAttrs, shadowAttrs).extractOpt[Int]

      Root(id = album.form.id,
           name = name,
           images = images.map(IlluminatedImageResponse.build),
           position = position,
           createdAt = model.createdAt,
           updatedAt = model.updatedAt)
    }
  }
}
