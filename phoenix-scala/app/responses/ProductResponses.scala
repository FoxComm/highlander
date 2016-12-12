package responses

import java.time.Instant

import cats.implicits._
import models.inventory._
import models.objects._
import models.product._
import responses.AlbumResponses._
import responses.ObjectResponses._
import responses.SkuResponses._
import responses.TaxonomyResponses.SingleTaxonResponse
import responses.VariantResponses._
import utils.aliases._

object ProductResponses {

  object ProductHeadResponse {

    case class Root(id: Int) extends ResponseItem

    //Product here is a placeholder for future. Using only form
    def build(p: Product): Root = Root(id = p.formId)
  }

  object ProductFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant, archivedAt: Option[Instant])
        extends ResponseItem

    //Product here is a placeholder for future. Using only form
    def build(p: Product, f: ObjectForm): Root =
      Root(id = f.id, attributes = f.attributes, createdAt = p.createdAt, p.archivedAt)
  }

  object ProductShadowResponse {

    case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant)
        extends ResponseItem

    def build(p: ObjectShadow): Root =
      Root(id = p.id, formId = p.formId, attributes = p.attributes, createdAt = p.createdAt)
  }

  object IlluminatedProductResponse {

    case class Root(id: Int, context: Option[ObjectContextResponse.Root], attributes: Json)
        extends ResponseItem

    def build(p: IlluminatedProduct): Root =
      Root(id = p.id,
           context = ObjectContextResponse.build(p.context).some,
           attributes = p.attributes)
    def buildLite(p: IlluminatedProduct): Root =
      Root(id = p.id, context = None, attributes = p.attributes)
  }

  object FullProductFormResponse {

    case class Root(product: ProductFormResponse.Root, skus: Seq[SkuFormResponse.Root])
        extends ResponseItem

    def build(product: Product,
              productForm: ObjectForm,
              skus: Seq[(ProductVariant, ObjectForm)]): Root =
      Root(product = ProductFormResponse.build(product, productForm), skus = skus.map {
        case (s, f) ⇒ SkuFormResponse.build(s, f)
      })
  }

  object FullProductShadowResponse {

    case class Root(product: ProductShadowResponse.Root, skus: Seq[SkuShadowResponse.Root])
        extends ResponseItem

    def build(shadow: ObjectShadow, skus: Seq[(ProductVariant, ObjectShadow)]): Root =
      Root(product = ProductShadowResponse.build(shadow), skus = skus.map {
        case (s, ss) ⇒ SkuShadowResponse.build(s, ss)
      })
  }

  object FullProductResponse {
    case class Root(form: FullProductFormResponse.Root, shadow: FullProductShadowResponse.Root)
        extends ResponseItem

    def build(product: Product,
              productForm: ObjectForm,
              productShadow: ObjectShadow,
              skus: Seq[FullObject[ProductVariant]]): Root =
      Root(form = FullProductFormResponse.build(product, productForm, skus.map { sku ⇒
        (sku.model, sku.form)
      }), shadow = FullProductShadowResponse.build(productShadow, skus.map { sku ⇒
        (sku.model, sku.shadow)
      }))
  }

  // New Product Response
  object ProductResponse {

    case class Root(id: Int,
                    context: ObjectContextResponse.Root,
                    attributes: Json,
                    albums: Seq[AlbumResponse.Root],
                    skus: Seq[SkuResponse.Root],
                    variants: Seq[IlluminatedVariantResponse.Root],
                    archivedAt: Option[Instant],
                    taxons: Seq[SingleTaxonResponse])
        extends ResponseItem

    def build(product: IlluminatedProduct,
              albums: Seq[AlbumResponse.Root],
              skus: Seq[SkuResponse.Root],
              variants: Seq[IlluminatedVariantResponse.Root],
              taxons: Seq[SingleTaxonResponse]): Root =
      Root(id = product.id,
           attributes = product.attributes,
           context = ObjectContextResponse.build(product.context),
           albums = albums,
           skus = skus,
           variants = variants,
           archivedAt = product.archivedAt,
           taxons = taxons)
  }
}
