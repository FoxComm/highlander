package responses

import SkuResponses._
import models.inventory._
import models.product._
import models.objects._
import models.Aliases.Json

import org.json4s.DefaultFormats
import org.json4s.Extraction
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.Serialization.{write ⇒ render}

import java.time.Instant
import cats.implicits._

object ProductResponses {

  object ObjectContextResponse { 

    final case class Root(name: String, attributes: Json) extends ResponseItem

    def build(c: ObjectContext) : Root = 
      Root(name = c.name, attributes = c.attributes)

    def build(c: IlluminatedContext) : Root = 
      Root(name = c.name, attributes = c.attributes)
  }

  object ProductFormResponse {

    final case class Root(id: Int, attributes: Json, createdAt: Instant)
      extends ResponseItem

    //Product here is a placeholder for future. Using only form
    def build(p: Product, f: ObjectForm): Root = 
      Root(id = f.id, attributes = f.attributes, createdAt = p.createdAt)
  }

  object ProductShadowResponse {

    final case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant)
      extends ResponseItem

    def build(p: ObjectShadow): Root = 
      Root(id = p.id, formId = p.formId, attributes = p.attributes, createdAt = p.createdAt)
  }

  object IlluminatedProductResponse {

    final case class Root(id: Int, context: Option[ObjectContextResponse.Root], 
      attributes: Json) extends ResponseItem

    def build(p: IlluminatedProduct): Root = 
      Root(id = p.id, context = ObjectContextResponse.build(p.context).some, 
        attributes = p.attributes)
    def buildLite(p: IlluminatedProduct): Root = 
      Root(id = p.id, context = None, attributes = p.attributes)
  }

  object FullProductFormResponse { 

    final case class Root(
      product: ProductFormResponse.Root,
      skus: Seq[SkuFormResponse.Root]) extends ResponseItem

    def build(product: Product, productForm: ObjectForm, 
      skus: Seq[(Sku, ObjectForm)]) : Root = 
      Root(
        product = ProductFormResponse.build(product, productForm),
        skus = skus.map { case (s, f) ⇒ SkuFormResponse.build(s, f) })
  }

  object FullProductShadowResponse { 

    final case class Root(
      product: ProductShadowResponse.Root,
      skus: Seq[SkuShadowResponse.Root]) extends ResponseItem

    def build(shadow: ObjectShadow, skus: Seq[(Sku, ObjectShadow)]) : Root = 
      Root(
        product = ProductShadowResponse.build(shadow),
        skus = skus.map { case (s, ss) ⇒  SkuShadowResponse.build(s, ss) })
  }

  object FullProductResponse { 
    final case class Root(form: FullProductFormResponse.Root, shadow: FullProductShadowResponse.Root)
      extends ResponseItem

    def build(
      product: Product,
      productForm: ObjectForm, 
      productShadow: ObjectShadow,
      skus: Seq[(Sku, ObjectForm, ObjectShadow)]): Root =
        Root(
          form = FullProductFormResponse.build(product, productForm, skus.map{ t ⇒ (t._1, t._2)}),
          shadow = FullProductShadowResponse.build(productShadow, skus.map{ t ⇒ (t._1, t._3)}))
  }

  object IlluminatedFullProductResponse {

    final case class Root(id: Int, context: ObjectContextResponse.Root, product: IlluminatedProductResponse.Root, 
      skus: Seq[IlluminatedSkuResponse.Root]) extends ResponseItem

    def build(p: IlluminatedProduct, skus: Seq[IlluminatedSku]): Root = 
      Root(
        id = p.id, 
        product = IlluminatedProductResponse.buildLite(p),
        context = ObjectContextResponse.build(p.context),
        skus = skus.map{ s ⇒ IlluminatedSkuResponse.buildLite(s)})
  }

}
