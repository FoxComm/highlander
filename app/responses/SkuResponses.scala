package responses

import java.time.Instant

import cats.implicits._
import models.Aliases.Json
import models.inventory._
import models.objects._
import responses.ObjectResponses.ObjectContextResponse

object SkuResponses {

  object SkuHeadResponse {

    case class Root(code: String) extends ResponseItem

    def build(sku: Sku): Root =
      Root(code = sku.code)
  }

  object SkuFormResponse {

    case class Root(code: String, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(sku: Sku, form: ObjectForm): Root =
      Root(code = sku.code, attributes = form.attributes, createdAt = form.createdAt)
  }

  object SkuShadowResponse {

    case class Root(code: String, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(sku: Sku, shadow: ObjectShadow): Root =
      Root(code = sku.code, attributes = shadow.attributes, createdAt = shadow.createdAt)
  }

  object IlluminatedSkuResponse {

    case class Root(code: String, context: Option[ObjectContextResponse.Root], attributes: Json)
        extends ResponseItem

    def build(s: IlluminatedSku): Root =
      Root(code = s.code,
           attributes = s.attributes,
           context = ObjectContextResponse.build(s.context).some)
    def buildLite(s: IlluminatedSku): Root =
      Root(code = s.code, attributes = s.attributes, context = None)
  }

  object FullSkuResponse {

    case class Root(code: String,
                    context: ObjectContextResponse.Root,
                    form: SkuFormResponse.Root,
                    shadow: SkuShadowResponse.Root)

    def build(
        form: SkuFormResponse.Root, shadow: SkuShadowResponse.Root, context: ObjectContext): Root =
      Root(code = shadow.code,
           form = form,
           shadow = shadow,
           context = ObjectContextResponse.build(context))
  }
}
