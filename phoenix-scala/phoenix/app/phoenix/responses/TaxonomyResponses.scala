package phoenix.responses

import objectframework.IlluminateAlgorithm
import objectframework.models.FullObject
import org.json4s.JsonAST.JValue
import phoenix.models.taxonomy.Taxonomy
import phoenix.responses.TaxonResponses._

object TaxonomyResponses {

  case class TaxonomyResponse(id: Int) extends ResponseItem

  object TaxonomyResponse {
    //Taxonomy here is a placeholder for future. Using only form
    def build(t: Taxonomy): TaxonomyResponse = TaxonomyResponse(id = t.formId)
  }

  case class FullTaxonomyResponse(id: Int,
                                  hierarchical: Boolean,
                                  attributes: JValue,
                                  taxons: Seq[TaxonTreeResponse])
      extends ResponseItem

  object FullTaxonomyResponse {
    def build(taxonomy: FullObject[Taxonomy], taxons: Seq[LinkedTaxon]): FullTaxonomyResponse =
      FullTaxonomyResponse(
        taxonomy.model.formId,
        taxonomy.model.hierarchical,
        IlluminateAlgorithm.projectAttributes(taxonomy.form.attributes, taxonomy.shadow.attributes),
        TaxonTreeResponse.buildTree(taxons, taxonomy.model.formId)
      )
  }
}
