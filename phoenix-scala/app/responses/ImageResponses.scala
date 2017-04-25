package responses

import models.image.Image
import models.objects.FullObject
import utils.{IlluminateAlgorithm, JsonFormatters}

object ImageResponses {
  implicit val formats = JsonFormatters.phoenixFormats

  case class S3ImageResponse(s3url: String) extends ResponseItem

  object ImageResponse {
    case class Root(id: Int,
                    src: String,
                    baseUrl: Option[String],
                    title: Option[String],
                    alt: Option[String])
        extends ResponseItem

    def build(id: Int,
              src: String,
              baseUrl: Option[String] = None,
              title: Option[String] = None,
              alt: Option[String] = None): ImageResponse.Root =
      Root(id, src, baseUrl, title, alt)

    def build(value: FullObject[Image]): ImageResponse.Root = {
      val form   = value.form.attributes
      val shadow = value.shadow.attributes

      val src     = IlluminateAlgorithm.get("src", form, shadow).extract[String]
      val baseUrl = IlluminateAlgorithm.get("baseUrl", form, shadow).extractOpt[String]
      val title   = IlluminateAlgorithm.get("title", form, shadow).extractOpt[String]
      val alt     = IlluminateAlgorithm.get("alt", form, shadow).extractOpt[String]

      build(value.model.id, src, baseUrl, title, alt)
    }
  }
}
