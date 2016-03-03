package responses

import models.product._
import models.Aliases.Json

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant

object ProductResponses {

  object ProductContextResponse { 

    final case class Root(name: String, attributes: Json)

    def build(c: ProductContext) : Root = 
      Root(name = c.name, attributes = c.attributes)

    def build(c: IlluminatedContext) : Root = 
      Root(name = c.name, attributes = c.attributes)
  }

  object ProductFormResponse {

    final case class Root(id: Int, attributes: Json, variants: Json, 
      isActive: Boolean, createdAt: Instant)

    def build(p: Product): Root = 
      Root(id = p.id, attributes = p.attributes, variants = p.variants, 
        isActive = p.isActive, createdAt = p.createdAt)
  }

  object ProductShadowResponse {


    final case class Root(id: Int, context: ProductContextResponse.Root, attributes: Json, 
      createdAt: Instant)

    def build(p: ProductShadow, c: ProductContext): Root = 
      Root(id = p.id, attributes = p.attributes, 
        context = ProductContextResponse.build(c),
        createdAt = p.createdAt)
  }

  object IlluminatedProductResponse {

    final case class Root(id: Int, context: ProductContextResponse.Root, attributes: Json, variants: Json)

    def build(p: IlluminatedProduct): Root = 
      Root(id = p.productId, attributes = p.attributes, variants = p.variants,
        context = ProductContextResponse.build(p.context))
  }
}
