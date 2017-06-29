package phoenix.responses

import java.time.Instant

import objectframework.ObjectResponses._
import phoenix.models.product._
import phoenix.responses.AlbumResponses._
import phoenix.responses.SkuResponses._
import phoenix.responses.TaxonResponses.AssignedTaxonsResponse
import phoenix.responses.VariantResponses._
import phoenix.utils.aliases._

object ProductResponses {

  case class ProductHeadResponse(id: Int) extends ResponseItem

  object ProductHeadResponse {

    //Product here is a placeholder for future. Using only form
    def build(p: Product): ProductHeadResponse = ProductHeadResponse(id = p.formId)
  }

  // New Product Response

  case class ProductResponse(id: Int,
                             slug: String,
                             context: ObjectContextResponse,
                             attributes: Json,
                             albums: Seq[AlbumResponse],
                             skus: Seq[SkuResponse],
                             variants: Seq[IlluminatedVariantResponse],
                             archivedAt: Option[Instant],
                             taxons: Seq[AssignedTaxonsResponse])
      extends ResponseItem

  object ProductResponse {

    def build(product: IlluminatedProduct,
              albums: Seq[AlbumResponse],
              skus: Seq[SkuResponse],
              variants: Seq[IlluminatedVariantResponse],
              taxons: Seq[AssignedTaxonsResponse]): ProductResponse =
      ProductResponse(
        id = product.id,
        slug = product.slug,
        attributes = product.attributes,
        context = ObjectContextResponse.build(product.context),
        albums = albums,
        skus = skus,
        variants = variants,
        archivedAt = product.archivedAt,
        taxons = taxons
      )
  }
}
