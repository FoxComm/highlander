package responses

import com.github.tminglei.slickpg.LTree
import models.objects.FullObject
import models.taxonomy._
import utils.IlluminateAlgorithm
import utils.aliases.Json

object TaxonomyResponses {

  type TaxonList  = Seq[TaxonResponse]
  type LinkedTerm = (FullObject[Taxon], TaxonomyTaxonLink)

  implicit class LTreeExtension(ltree: LTree) {
    def level = ltree.value match {
      case List("") ⇒ 0
      case list     ⇒ list.size
    }

    def parentOf(other: LTree): Boolean = {
      if (level == 0) other.level == 1 else other.value.startsWith(ltree.value)
    }
  }

  case class TaxonomyResponse(id: Int, hierarchical: Boolean, attributes: Json, taxons: TaxonList)

  object TaxonomyResponse {
    def build(taxon: FullObject[Taxonomy], terms: Seq[LinkedTerm]): TaxonomyResponse = {
      TaxonomyResponse(
          taxon.model.formId,
          taxon.model.hierarchical,
          IlluminateAlgorithm.projectAttributes(taxon.form.attributes, taxon.shadow.attributes),
          TaxonResponse.buildTree(terms))
    }
  }

  case class SingleTaxonResponse(taxonomyId: Int,
                                 taxon: TaxonResponse,
                                 parentTaxonId: Option[Integer])
  object SingleTaxonResponse {
    def build(taxonomyId: Integer, taxon: FullObject[Taxon], parentTaxonId: Option[Integer]) =
      SingleTaxonResponse(taxonomyId, TaxonResponse.build(taxon), parentTaxonId)
  }

  case class TaxonResponse(id: Int, attributes: Json, children: TaxonList)

  object TaxonResponse {

    def build(taxon: FullObject[Taxon]): TaxonResponse = {
      TaxonResponse(
          taxon.model.formId,
          IlluminateAlgorithm.projectAttributes(taxon.form.attributes, taxon.shadow.attributes),
          Seq())
    }

    def buildTree(nodes: Seq[LinkedTerm]): TaxonList =
      buildTree(0, nodes.sortBy { case (_, link) ⇒ link.path.level })

    private def buildTree(level: Int, nodesSorted: Seq[LinkedTerm]): TaxonList = {
      val (heads, tail)   = nodesSorted.span { case (_, link) ⇒ link.path.level == level }
      val headsByPosition = heads.sortBy { case (_, link)     ⇒ link.position }
      headsByPosition.map {
        case (term, link) ⇒
          TaxonResponse(
              term.model.formId,
              IlluminateAlgorithm.projectAttributes(term.form.attributes, term.shadow.attributes),
              buildTree(level + 1, tail.filter {
                case (_, lnk) ⇒ lnk.parentIndex.contains(link.index)
              }))
      }
    }
  }
}
