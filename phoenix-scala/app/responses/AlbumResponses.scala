package responses

import cats.implicits._
import io.circe.syntax._
import java.time.Instant
import models.image._
import models.objects._
import responses.ImageResponses.ImageResponse
import services.image.ImageManager.FullAlbumWithImages
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.json.codecs._
import utils.json.yolo._

object AlbumResponses {
  object AlbumResponse {
    case class Root(id: Int,
                    name: String,
                    images: Seq[ImageResponse.Root],
                    createdAt: Instant,
                    updatedAt: Instant,
                    archivedAt: Option[Instant])
        extends ResponseItem {
      def json: Json = this.asJson
    }

    def build(album: FullObject[Album], images: Seq[FullObject[Image]]): Root = {
      val model       = album.model
      val formAttrs   = album.form.attributes
      val shadowAttrs = album.shadow.attributes

      val name = IlluminateAlgorithm.get("name", formAttrs, shadowAttrs).orEmpty.extract[String]

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
