package responses

import com.github.tminglei.slickpg.LTree
import models.objects.FullObject
import models.taxonomy.{Taxon ⇒ ModelTaxon, _}
import utils.{IlluminateAlgorithm, JsonFormatters}
import utils.aliases.Json

object TaxonomyResponses {

  type TaxonList   = Seq[TaxonTreeResponse]
  type LinkedTaxon = (FullObject[ModelTaxon], TaxonomyTaxonLink)

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
    def build(taxon: FullObject[Taxonomy], taxons: Seq[LinkedTaxon]): TaxonomyResponse = {
      TaxonomyResponse(
          taxon.model.formId,
          taxon.model.hierarchical,
          IlluminateAlgorithm.projectAttributes(taxon.form.attributes, taxon.shadow.attributes),
          TaxonTreeResponse.buildTree(taxons))
    }
  }

  case class SingleTaxonResponse(taxonomyId: Int, taxon: Taxon, parentId: Option[Integer])
  object SingleTaxonResponse {
    def build(taxonomyId: Integer, taxon: FullObject[ModelTaxon], parentTaxonId: Option[Integer]) =
      SingleTaxonResponse(taxonomyId, Taxon.build(taxon), parentTaxonId)
  }

  case class TaxonTreeResponse(taxon: Taxon, children: TaxonList)

  case class Taxon(id: Int, name: String)

  object Taxon {
    implicit val formats = JsonFormatters.phoenixFormats

    def build(taxon: FullObject[ModelTaxon]): Taxon = {
      Taxon(taxon.model.formId,
            IlluminateAlgorithm
              .get("name", taxon.form.attributes, taxon.shadow.attributes)
              .extract[String])
    }
  }

  object TaxonTreeResponse {
    def build(taxon: FullObject[ModelTaxon]): TaxonTreeResponse = {
      TaxonTreeResponse(Taxon.build(taxon), Seq())
    }

    def buildTree(nodes: Seq[LinkedTaxon]): TaxonList =
      buildTree(0, nodes.sortBy { case (_, link) ⇒ link.path.level })

    private def buildTree(level: Int, nodesSorted: Seq[LinkedTaxon]): TaxonList = {
      val (heads, tail)   = nodesSorted.span { case (_, link) ⇒ link.path.level == level }
      val headsByPosition = heads.sortBy { case (_, link)     ⇒ link.position }
      headsByPosition.map {
        case (taxon, link) ⇒
          TaxonTreeResponse(Taxon.build(taxon), buildTree(level + 1, tail.filter {
            case (_, lnk) ⇒ lnk.parentIndex.contains(link.index)
          }))
      }
    }
  }
}
