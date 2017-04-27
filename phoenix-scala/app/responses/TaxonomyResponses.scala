package responses

import io.circe.syntax._
import models.objects.FullObject
import models.taxonomy.Taxonomy
import responses.TaxonResponses._
import utils.IlluminateAlgorithm
import utils.aliases._
import utils.json.codecs._

object TaxonomyResponses {

  object TaxonomyResponse {
    case class Root(id: Int) extends ResponseItem {
      def json: Json = this.asJson
    }

    //Taxonomy here is a placeholder for future. Using only form
    def build(t: Taxonomy): Root = Root(id = t.formId)
  }

  case class FullTaxonomyResponse(id: Int,
                                  hierarchical: Boolean,
                                  attributes: Json,
                                  taxons: Seq[TaxonTreeResponse])
      extends ResponseItem {
    def json: Json = this.asJson
  }

  object FullTaxonomyResponse {
    def build(taxonomy: FullObject[Taxonomy], taxons: Seq[LinkedTaxon]): FullTaxonomyResponse = {
      FullTaxonomyResponse(taxonomy.model.formId,
                           taxonomy.model.hierarchical,
                           IlluminateAlgorithm.projectAttributes(taxonomy.form.attributes,
                                                                 taxonomy.shadow.attributes),
                           TaxonTreeResponse.buildTree(taxons, taxonomy.model.formId))
    }
  }
}
