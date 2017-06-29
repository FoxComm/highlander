package phoenix.responses

import java.time.Instant

import objectframework.IlluminateAlgorithm
import objectframework.models.FullObject
import phoenix.models.image.Image
import phoenix.utils.JsonFormatters

object ImageResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  case class ImageResponse(id: Int,
                           src: String,
                           createdAt: Instant,
                           baseUrl: Option[String],
                           title: Option[String],
                           alt: Option[String])
      extends ResponseItem

  object ImageResponse {

    def build(id: Int,
              src: String,
              createdAt: Instant,
              baseUrl: Option[String] = None,
              title: Option[String] = None,
              alt: Option[String] = None): ImageResponse =
      ImageResponse(id, src, createdAt, baseUrl, title, alt)

    def build(value: FullObject[Image]): ImageResponse = {
      val form   = value.form.attributes
      val shadow = value.shadow.attributes

      val src     = IlluminateAlgorithm.get("src", form, shadow).extract[String]
      val baseUrl = IlluminateAlgorithm.get("baseUrl", form, shadow).extractOpt[String]
      val title   = IlluminateAlgorithm.get("title", form, shadow).extractOpt[String]
      val alt     = IlluminateAlgorithm.get("alt", form, shadow).extractOpt[String]

      build(value.model.id, src, value.form.createdAt, baseUrl, title, alt)
    }
  }
}
