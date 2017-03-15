package responses

import models.objects.FullObject
import models.taxonomy.Taxonomy
import responses.TaxonResponses._
import utils.{IlluminateAlgorithm}
import utils.aliases.Json

object TaxonomyResponses {

  object TaxonomyResponse {
    case class Root(id: Int) extends ResponseItem

    //Taxonomy here is a placeholder for future. Using only form
    def build(t: Taxonomy): Root = Root(id = t.formId)
  }

  case class FullTaxonomyResponse(id: Int,
                                  hierarchical: Boolean,
                                  attributes: Json,
                                  taxons: Seq[TaxonTreeResponse])
      extends ResponseItem

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
