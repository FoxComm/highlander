package responses

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import models.objects.FullObject
import models.taxonomy._
import utils.IlluminateAlgorithm
import utils.aliases.Json

object TaxonResponses {

  type LinkedTaxon = (FullObject[Taxon], TaxonomyTaxonLink)

  implicit class LTreeExtension(ltree: LTree) {
    def level = ltree.value match {
      case List("") ⇒ 0
      case list     ⇒ list.size
    }

    def parentOf(other: LTree): Boolean = {
      if (level == 0) other.level == 1 else other.value.startsWith(ltree.value)
    }
  }

  case class AssignedTaxonsResponse(taxonomyId: Int,
                                    hierarchical: Boolean,
                                    attributes: Json,
                                    taxons: Seq[FullTaxonResponse])
      extends ResponseItem

  object AssignedTaxonsResponse {
    def build(taxonomy: FullObject[Taxonomy],
              taxons: Seq[FullObject[Taxon]]): AssignedTaxonsResponse = {
      val taxonAttributes =
        IlluminateAlgorithm.projectAttributes(taxonomy.form.attributes, taxonomy.shadow.attributes)

      AssignedTaxonsResponse(taxonomy.model.formId,
                             taxonomy.model.hierarchical,
                             taxonAttributes,
                             taxons.map(FullTaxonResponse.build(_, taxonomy.model.formId)))
    }
  }

  object TaxonResponse {
    case class Root(id: Int) extends ResponseItem

    //Taxon here is a placeholder for future. Using only form
    def build(t: Taxon): Root = Root(id = t.formId)
  }

  case class FullTaxonResponse(id: Int,
                               taxonomyId: Int,
                               parentId: Option[Integer],
                               attributes: Json)
      extends ResponseItem

  object FullTaxonResponse {
    def build(taxon: FullObject[Taxon],
              taxonomyId: Int,
              parentId: Option[Integer] = None): FullTaxonResponse = {
      FullTaxonResponse(
          taxon.model.formId,
          taxonomyId,
          parentId,
          IlluminateAlgorithm.projectAttributes(taxon.form.attributes, taxon.shadow.attributes))
    }
  }

  case class TaxonTreeResponse(taxon: FullTaxonResponse, children: Option[Seq[TaxonTreeResponse]])
      extends ResponseItem {
    def childrenAsList: Seq[TaxonTreeResponse] = children.getOrElse(Seq.empty)
  }

  object TaxonTreeResponse {
    def build(taxon: FullTaxonResponse, children: Seq[TaxonTreeResponse]): TaxonTreeResponse = {
      TaxonTreeResponse(taxon, children.some.filterNot(_.isEmpty))
    }

    def buildTree(nodes: Seq[LinkedTaxon], taxonomyId: Int): Seq[TaxonTreeResponse] =
      buildTree(0, nodes.sortBy { case (_, link) ⇒ link.path.level }, taxonomyId)

    private def buildTree(level: Int,
                          nodesSorted: Seq[LinkedTaxon],
                          taxonomyId: Int): Seq[TaxonTreeResponse] = {
      val (heads, tail)   = nodesSorted.span { case (_, link) ⇒ link.path.level == level }
      val headsByPosition = heads.sortBy { case (_, link)     ⇒ link.position }
      headsByPosition.map {
        case (taxon, link) ⇒
          TaxonTreeResponse.build(FullTaxonResponse.build(taxon, taxonomyId),
                                  buildTree(level + 1, tail.filter {
                                    case (_, lnk) ⇒ lnk.path.value.startsWith(link.childPath.value)
                                  }, taxonomyId))
      }
    }
  }
}
