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

  object ProductHeadResponse {

    case class Root(id: Int) extends ResponseItem

    //Product here is a placeholder for future. Using only form
    def build(p: Product): Root = Root(id = p.formId)
  }

  // New Product Response
  object ProductResponse {

    case class Root(id: Int,
                    slug: String,
                    context: ObjectContextResponse.Root,
                    attributes: Json,
                    albums: Seq[AlbumResponse.Root],
                    skus: Seq[SkuResponse.Root],
                    variants: Seq[IlluminatedVariantResponse.Root],
                    archivedAt: Option[Instant],
                    taxons: Seq[AssignedTaxonsResponse])
        extends ResponseItem

    def build(product: IlluminatedProduct,
              albums: Seq[AlbumResponse.Root],
              skus: Seq[SkuResponse.Root],
              variants: Seq[IlluminatedVariantResponse.Root],
              taxons: Seq[AssignedTaxonsResponse]): Root =
      Root(id = product.id,
           slug = product.slug,
           attributes = product.attributes,
           context = ObjectContextResponse.build(product.context),
           albums = albums,
           skus = skus,
           variants = variants,
           archivedAt = product.archivedAt,
           taxons = taxons)
  }
}
