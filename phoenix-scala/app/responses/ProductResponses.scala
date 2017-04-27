package responses

import io.circe.syntax._
import java.time.Instant
import models.product._
import responses.AlbumResponses._
import responses.ObjectResponses._
import responses.SkuResponses._
import responses.TaxonResponses.AssignedTaxonsResponse
import responses.VariantResponses._
import utils.aliases._
import utils.json.codecs._

object ProductResponses {

  object ProductHeadResponse {

    case class Root(id: Int) extends ResponseItem {
      def json: Json = this.asJson
    }

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
        extends ResponseItem {
      def json: Json = this.asJson
    }

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
