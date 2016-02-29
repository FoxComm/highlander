package responses

import models.product._
import models.product.Aliases.Json

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant

object ProductResponses {

  final case class Context(name: String, attributes: Json)

  object ProductFormResponse {

    final case class Root(id: Int, attributes: Json, variants: Json, 
      isActive: Boolean, createdAt: Instant)

    def build(p: Product): Root = 
      Root(id = p.id, attributes = p.attributes, variants = p.variants, 
        isActive = p.isActive, createdAt = p.createdAt)
  }

  object ProductShadowResponse {


    final case class Root(id: Int, context: Context, attributes: Json, 
      createdAt: Instant)

    def build(p: ProductShadow, c: ProductContext): Root = 
      Root(id = p.id, attributes = p.attributes, 
        context = Context(name = c.name, attributes = c.attributes),
        createdAt = p.createdAt)
  }

  object IlluminatedProductResponse {

    final case class Root(id: Int, context: Context, attributes: Json, variants: Json)

    def build(p: IlluminatedProduct): Root = 
      Root(id = p.productId, attributes = p.attributes, variants = p.variants,
        context = Context(name = p.context.name, attributes = p.context.attributes))
  }
}
