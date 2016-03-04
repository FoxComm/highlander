package responses

import SkuResponses._
import models.inventory._
import models.product._
import models.Aliases.Json

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant
import cats.implicits._

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

    final case class Root(id: Int, productId: Int, context: ProductContextResponse.Root, 
      attributes: Json, createdAt: Instant)

    def build(p: ProductShadow, c: ProductContext): Root = 
      Root(id = p.id, productId = p.productId, 
        attributes = p.attributes, 
        context = ProductContextResponse.build(c),
        createdAt = p.createdAt)
  }

  object IlluminatedProductResponse {

    final case class Root(id: Int, context: Option[ProductContextResponse.Root], attributes: Json, variants: Json)

    def build(p: IlluminatedProduct): Root = 
      Root(id = p.productId, attributes = p.attributes, variants = p.variants,
        context = ProductContextResponse.build(p.context).some)
    def buildLite(p: IlluminatedProduct): Root = 
      Root(id = p.productId, attributes = p.attributes, variants = p.variants,
        context = None)
  }

  object FullProductFormResponse { 

    final case class Root(
      product: ProductFormResponse.Root,
      skus: Seq[SkuFormResponse.Root])

    def build(product: Product, skus: Seq[Sku]) : Root = 
      Root(
        product = ProductFormResponse.build(product),
        skus = skus.map { s ⇒ SkuFormResponse.build(s) })
  }

  object FullProductShadowResponse { 

    final case class Root(
      product: ProductShadowResponse.Root,
      skus: Seq[SkuShadowLiteResponse.Root])

    def build(context: ProductContext, product: ProductShadow, skus: Seq[(Sku, SkuShadow)]) : Root = 
      Root(
        product = ProductShadowResponse.build(product, context),
        skus = skus.map { case (s, ss) ⇒  SkuShadowLiteResponse.build(s, ss) })
  }

  object FullIlluminatedProductResponse { 

    final case class Root(
      product: IlluminatedProductResponse.Root,
      skus: Seq[IlluminatedSkuResponse.Root])

    def build(product: IlluminatedProduct, skus: Seq[IlluminatedSku]) : Root = 
      Root(
        product = IlluminatedProductResponse.build(product),
        skus = skus.map { s ⇒  IlluminatedSkuResponse.build(s) })
  }

  object IlluminatedFullProductResponse {

    final case class Root(id: Int, context: ProductContextResponse.Root, product: IlluminatedProductResponse.Root, 
      skus: Seq[IlluminatedSkuResponse.Root])

    def build(p: IlluminatedProduct, skus: Seq[IlluminatedSku]): Root = 
      Root(
        id = p.productId, 
        product = IlluminatedProductResponse.buildLite(p),
        context = ProductContextResponse.build(p.context),
        skus = skus.map{ s ⇒ IlluminatedSkuResponse.buildLite(s)})
  }

}
