package responses

import java.time.Instant

import models.image._
import models.objects._
import responses.ImageResponses.ImageResponse
import services.image.ImageManager.FullAlbumWithImages
import utils.{IlluminateAlgorithm, JsonFormatters}

object AlbumResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  object AlbumResponse {
    case class Root(id: Int,
                    name: String,
                    images: Seq[ImageResponse.Root],
                    createdAt: Instant,
                    updatedAt: Instant,
                    archivedAt: Option[Instant])
        extends ResponseItem

    def build(album: FullObject[Album], images: Seq[FullObject[Image]]): Root = {
      val model       = album.model
      val formAttrs   = album.form.attributes
      val shadowAttrs = album.shadow.attributes

      val name = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).extract[String]

      Root(id = album.form.id,
           name = name,
           images = images.map(ImageResponse.build),
           createdAt = model.createdAt,
           updatedAt = model.updatedAt,
           archivedAt = model.archivedAt)
    }

    def build(fullAlbum: FullAlbumWithImages): Root = {
      val (album, images) = fullAlbum
      build(album, images)
    }
  }
}
