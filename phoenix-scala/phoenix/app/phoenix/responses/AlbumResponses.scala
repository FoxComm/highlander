package phoenix.responses

import java.time.Instant

import objectframework.IlluminateAlgorithm
import objectframework.models._
import phoenix.models.image._
import phoenix.responses.ImageResponses.ImageResponse
import phoenix.services.image.ImageManager.FullAlbumWithImages
import phoenix.utils.JsonFormatters

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
