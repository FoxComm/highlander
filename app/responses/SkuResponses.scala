package responses

import ProductResponses.ProductContextResponse
import models.inventory._
import models.product.ProductContext
import models.Aliases.Json

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant
import cats.implicits._

object SkuResponses {

  object SkuFormResponse { 

    final case class Root(
      code: String,
      attributes: Json,
      createdAt: Instant)

    def build(sku: Sku) : Root =
      Root(code = sku.code, attributes = sku.attributes, createdAt = sku.createdAt)
  }

  object SkuShadowResponse { 

    final case class Root(code: String, attributes: Json, activeFrom: Option[Instant], 
      activeTo: Option[Instant], createdAt: Instant)

    def build(sku: Sku, skuShadow: SkuShadow) : Root =
      Root(code = sku.code, attributes = skuShadow.attributes, 
        activeFrom = skuShadow.activeFrom, activeTo = skuShadow.activeTo,
        createdAt = skuShadow.createdAt)
  }

  object IlluminatedSkuResponse {

    final case class Root(code: String, context: Option[ProductContextResponse.Root], 
      attributes: Json, activeFrom: Option[Instant], activeTo: Option[Instant])

    def build(s: IlluminatedSku): Root = 
      Root(code = s.code, attributes = s.attributes, 
        context = ProductContextResponse.build(s.context).some,
        activeFrom = s.activeFrom, activeTo = s.activeTo)
    def buildLite(s: IlluminatedSku): Root = 
      Root(code = s.code, attributes = s.attributes, context = None,
        activeFrom = s.activeFrom, activeTo = s.activeTo)
  }
}
