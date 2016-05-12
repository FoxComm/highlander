package responses

import java.time.Instant

import cats.implicits._
import models.Aliases.Json
import models.product._
import models.objects._
import responses.ObjectResponses.ObjectContextResponse
import responses.VariantValueResponses.IlluminatedVariantValueResponse

object VariantResponses {
  object IlluminatedVariantResponse {
    case class Root(id: Int, context: Option[ObjectContextResponse.Root],
      attributes: Json, values: Seq[IlluminatedVariantValueResponse.Root])
      extends ResponseItem

    def build(v: IlluminatedVariant, vs: Seq[FullObject[VariantValue]]): Root =
      Root(id = v.id, attributes = v.attributes,
        context = ObjectContextResponse.build(v.context).some,
        values = vs.map(IlluminatedVariantValueResponse.build _))

    def buildLite(v: IlluminatedVariant, vs: Seq[FullObject[VariantValue]]): Root =
      Root(id = v.id, attributes = v.attributes, context = None,
        values = vs.map(IlluminatedVariantValueResponse.build _))
  }
}
