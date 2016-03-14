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
      skus: Json, createdAt: Instant)

    def build(p: Product): Root = 
      Root(id = p.id, attributes = p.attributes, variants = p.variants, 
        skus = p.skus, createdAt = p.createdAt)
  }

  object ProductShadowResponse {

    final case class Root(id: Int, productId: Int, attributes: Json, variants: String,
      skus: String, activeFrom: Option[Instant], activeTo: Option[Instant], createdAt: Instant)

    def build(p: ProductShadow): Root = 
      Root(id = p.id, productId = p.productId, attributes = p.attributes, 
        variants = p.variants, skus = p.skus, activeFrom = p.activeFrom, 
        activeTo = p.activeTo, createdAt = p.createdAt)
  }

  object IlluminatedProductResponse {

    final case class Root(id: Int, context: Option[ProductContextResponse.Root], 
      attributes: Json, variants: Json, skus: Json, activeFrom: Option[Instant], 
      activeTo: Option[Instant])

    def build(p: IlluminatedProduct): Root = 
      Root(id = p.productId, attributes = p.attributes, variants = p.variants,
        skus = p.skus, context = ProductContextResponse.build(p.context).some,
        activeFrom = p.activeFrom, activeTo = p.activeTo)
    def buildLite(p: IlluminatedProduct): Root = 
      Root(id = p.productId, attributes = p.attributes, variants = p.variants,
        skus = p.skus, context = None, activeFrom = p.activeFrom, activeTo = p.activeTo)
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
      skus: Seq[SkuShadowResponse.Root])

    def build(product: ProductShadow, skus: Seq[(Sku, SkuShadow)]) : Root = 
      Root(
        product = ProductShadowResponse.build(product),
        skus = skus.map { case (s, ss) ⇒  SkuShadowResponse.build(s, ss) })
  }

  object FullProductResponse { 
    final case class Root(form: FullProductFormResponse.Root, shadow: FullProductShadowResponse.Root)

    def build(
      productForm: Product, 
      productShadow: ProductShadow,
      skus: Seq[(Sku, SkuShadow)]): Root =
        Root(
          form = FullProductFormResponse.build(productForm, skus.map(_._1)),
          shadow = FullProductShadowResponse.build(productShadow, skus))
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
