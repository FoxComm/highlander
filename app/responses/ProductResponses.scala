package responses

import java.time.Instant

import cats.implicits._
import models.Aliases.Json
import models.inventory._
import models.objects._
import models.product._
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses._
import responses.VariantResponses._

object ProductResponses {

  object ProductHeadResponse {

    case class Root(id: Int) extends ResponseItem

    //Product here is a placeholder for future. Using only form
    def build(p: Product): Root = Root(id = p.formId)
  }

  object ProductFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant)
      extends ResponseItem

    //Product here is a placeholder for future. Using only form
    def build(p: Product, f: ObjectForm): Root = 
      Root(id = f.id, attributes = f.attributes, createdAt = p.createdAt)
  }

  object ProductShadowResponse {

    case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant)
      extends ResponseItem

    def build(p: ObjectShadow): Root = 
      Root(id = p.id, formId = p.formId, attributes = p.attributes, createdAt = p.createdAt)
  }

  object IlluminatedProductResponse {

    case class Root(id: Int, context: Option[ObjectContextResponse.Root], 
      attributes: Json) extends ResponseItem

    def build(p: IlluminatedProduct): Root = 
      Root(id = p.id, context = ObjectContextResponse.build(p.context).some, 
        attributes = p.attributes)
    def buildLite(p: IlluminatedProduct): Root = 
      Root(id = p.id, context = None, attributes = p.attributes)
  }

  object FullProductFormResponse { 

    case class Root(
      product: ProductFormResponse.Root,
      skus: Seq[SkuFormResponse.Root]) extends ResponseItem

    def build(product: Product, productForm: ObjectForm, 
      skus: Seq[(Sku, ObjectForm)]) : Root = 
      Root(
        product = ProductFormResponse.build(product, productForm),
        skus = skus.map { case (s, f) ⇒ SkuFormResponse.build(s, f) })
  }

  object FullProductShadowResponse { 

    case class Root(
      product: ProductShadowResponse.Root,
      skus: Seq[SkuShadowResponse.Root]) extends ResponseItem

    def build(shadow: ObjectShadow, skus: Seq[(Sku, ObjectShadow)]) : Root = 
      Root(
        product = ProductShadowResponse.build(shadow),
        skus = skus.map { case (s, ss) ⇒  SkuShadowResponse.build(s, ss) })
  }

  object FullProductResponse { 
    case class Root(form: FullProductFormResponse.Root, shadow: FullProductShadowResponse.Root)
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

    case class Root(id: Int, context: ObjectContextResponse.Root, product: IlluminatedProductResponse.Root,
      skus: Seq[IlluminatedSkuResponse.Root], variants: Seq[IlluminatedVariantResponse.Root], variantMap: Json)
      extends ResponseItem

    def build(p: IlluminatedProduct, skus: Seq[IlluminatedSku],
      variants: Seq[(IlluminatedVariant, Seq[FullObject[VariantValue]])],
      variantMap: Map[String, Seq[FullObject[VariantValue]]]): Root =
      Root(
        id = p.id, 
        product = IlluminatedProductResponse.buildLite(p),
        context = ObjectContextResponse.build(p.context),
        skus = skus.map(IlluminatedSkuResponse.buildLite _),
        variants = variants.map { case (variant, values) ⇒ IlluminatedVariantResponse.buildLite(variant, values) },
        variantMap = buildVariantMap(variantMap))

    private def buildVariantMap(vm: Map[String, Seq[FullObject[VariantValue]]]): JValue = {
      val idMap = vm.mapValues(_.map(_.form.id))
      render(idMap)
    }
  }

}
