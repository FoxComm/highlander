package responses

import java.time.Instant

import cats.implicits._
import models.Aliases.Json
import models.product._
import models.objects._
import responses.ObjectResponses.ObjectContextResponse

object VariantResponses {
  object IlluminatedVariantResponse {
    case class Root(id: Int, context: Option[ObjectContextResponse.Root], attributes: Json)
      extends ResponseItem

    def build(v: IlluminatedVariant): Root =
      Root(id = v.id, attributes = v.attributes,
        context = ObjectContextResponse.build(v.context).some)

    def buildLite(v: IlluminatedVariant): Root =
      Root(id = v.id, attributes = v.attributes, context = None)
  }
}
