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
      isHazardous: Boolean,
      isActive: Boolean)

    def build(sku: Sku) : Root =
      Root(code = sku.code, attributes = sku.attributes, 
        isHazardous = sku.isHazardous, isActive = sku.isActive)
  }

  object SkuShadowLiteResponse { 

    final case class Root(
      code: String,
      attributes: Json,
      isHazardous: Boolean,
      isActive: Boolean)

    def build(sku: Sku, skuShadow: SkuShadow) : Root =
      Root(code = sku.code, attributes = skuShadow.attributes, 
        isHazardous = sku.isHazardous, isActive = sku.isActive)
  }

  object SkuShadowResponse { 

    final case class Root(
      code: String,
      context: ProductContextResponse.Root,
      attributes: Json,
      isHazardous: Boolean,
      isActive: Boolean)

    def build(sku: Sku, skuShadow: SkuShadow, context: ProductContext) : Root =
      Root(code = sku.code, attributes = skuShadow.attributes, isHazardous = sku.isHazardous,
        isActive = sku.isActive, context = ProductContextResponse.build(context))
  }

  object IlluminatedSkuResponse {

    final case class Root(code: String, context: Option[ProductContextResponse.Root], 
      attributes: Json)


    def build(s: IlluminatedSku): Root = 
      Root(code = s.code, attributes = s.attributes, 
        context = ProductContextResponse.build(s.context).some)
    def buildLite(s: IlluminatedSku): Root = 
      Root(code = s.code, attributes = s.attributes, context = None)
  }
}
