package responses

import responses.ObjectResponses.ObjectContextResponse
import models.inventory._
import models.objects._
import models.Aliases.Json

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant
import cats.implicits._

object SkuResponses {

  object SkuHeadResponse { 

    final case class Root(code: String) 
      extends ResponseItem

    def build(sku: Sku) : Root =
      Root(code = sku.code)
  }

  object SkuFormResponse { 

    final case class Root(code: String, attributes: Json, createdAt: Instant) 
      extends ResponseItem

    def build(sku: Sku, form: ObjectForm) : Root =
      Root(code = sku.code, attributes = form.attributes, 
        createdAt = form.createdAt)
  }

  object SkuShadowResponse { 

    final case class Root(code: String, attributes: Json, createdAt: Instant) 
      extends ResponseItem

    def build(sku: Sku, shadow: ObjectShadow) : Root =
      Root(code = sku.code, attributes = shadow.attributes, 
        createdAt = shadow.createdAt)
  }

  object IlluminatedSkuResponse {

    final case class Root(code: String, context: Option[ObjectContextResponse.Root], 
      attributes: Json) extends ResponseItem

    def build(s: IlluminatedSku): Root = 
      Root(code = s.code, attributes = s.attributes, 
        context = ObjectContextResponse.build(s.context).some)
    def buildLite(s: IlluminatedSku): Root = 
      Root(code = s.code, attributes = s.attributes, context = None)
  }

  object IlluminatedFullSkuResponse {

    final case class Root(code: String, context: ObjectContextResponse.Root,
      form: SkuFormResponse.Root, shadow: SkuShadowResponse.Root)

    def build(form: SkuFormResponse.Root, shadow: SkuShadowResponse.Root,
      context: ObjectContext): Root =
      Root(
        code = shadow.code,
        form = form, shadow = shadow,
        context = ObjectContextResponse.build(context))
  }
}
