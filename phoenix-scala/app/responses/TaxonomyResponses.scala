package responses

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import models.objects.FullObject
import models.taxonomy.{Taxon, _}
import utils.{IlluminateAlgorithm, JsonFormatters}
import utils.aliases.Json

object TaxonomyResponses {

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

  case class AssignedTaxonsResponse(taxonomyId: Int,
                                    hierarchical: Boolean,
                                    attributes: Json,
                                    taxons: Seq[TaxonResponse])
      extends ResponseItem

  object AssignedTaxonsResponse {
    def build(taxonomy: FullObject[Taxonomy],
              taxons: Seq[FullObject[Taxon]]): AssignedTaxonsResponse = {
      val taxonAttributes =
        IlluminateAlgorithm.projectAttributes(taxonomy.form.attributes, taxonomy.shadow.attributes)

      AssignedTaxonsResponse(taxonomy.model.formId,
                             taxonomy.model.hierarchical,
                             taxonAttributes,
                             taxons.map(TaxonResponse.build(_, taxonomy.model.formId)))
    }
  }

  case class TaxonResponse(id: Int, taxonomyId: Int, parentId: Option[Integer], attributes: Json)
      extends ResponseItem

  object TaxonResponse {
    def build(taxon: FullObject[Taxon],
              taxonomyId: Int,
              parentId: Option[Integer] = None): TaxonResponse = {
      TaxonResponse(
          taxon.model.formId,
          taxonomyId,
          parentId,
          IlluminateAlgorithm.projectAttributes(taxon.form.attributes, taxon.shadow.attributes))
    }
  }

//  case class SingleTaxonResponse(taxonomyId: Int, taxon: TaxonResponse, parentId: Option[Integer])
//      extends ResponseItem
//
//  object SingleTaxonResponse {
//    def build(taxonomyId: Integer,
//              taxon: FullObject[Taxon],
//              parentTaxonId: Option[Integer]): SingleTaxonResponse =
//      SingleTaxonResponse(taxonomyId, TaxonResponse.build(taxon), parentTaxonId)
//  }

  case class TaxonTreeResponse(taxon: TaxonResponse, children: Option[Seq[TaxonTreeResponse]])
      extends ResponseItem {
    def childrenAsList: Seq[TaxonTreeResponse] = children.getOrElse(Seq.empty)
  }

//  case class TaxonResponse(id: Int, name: String) extends ResponseItem
//
//  object TaxonResponse {
//    implicit val formats = JsonFormatters.phoenixFormats
//
//    def build(taxon: FullObject[Taxon]): TaxonResponse = {
//      TaxonResponse(taxon.model.formId,
//                    IlluminateAlgorithm
//                      .get("name", taxon.form.attributes, taxon.shadow.attributes)
//                      .extract[String])
//    }
//  }

  object TaxonTreeResponse {
    def build(taxon: TaxonResponse, children: Seq[TaxonTreeResponse]): TaxonTreeResponse = {
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
          TaxonTreeResponse.build(TaxonResponse.build(taxon, taxonomyId),
                                  buildTree(level + 1, tail.filter {
                                    case (_, lnk) ⇒ lnk.path.value.startsWith(link.childPath.value)
                                  }, taxonomyId))
      }
    }
  }
}
