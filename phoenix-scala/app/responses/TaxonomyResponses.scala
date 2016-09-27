package responses

import com.github.tminglei.slickpg.LTree
import models.objects.FullObject
import models.taxonomy._
import utils.IlluminateAlgorithm
import utils.aliases.Json

object TaxonomyResponses {

  type TermList   = Seq[TermResponse.Root]
  type LinkedTerm = (FullObject[Term], TaxonTermLink)

  implicit class LTreeExtension(ltree: LTree) {
    def level = ltree.value match {
      case List("") ⇒ 0
      case list     ⇒ list.size
    }

    def parentOf(other: LTree): Boolean = {
      if (level == 0) other.level == 1 else other.value.startsWith(ltree.value)
    }
  }

  object TaxonResponse {

    def build(taxon: FullObject[Taxon], terms: Seq[LinkedTerm]): Root = {
      Root(taxon.model.formId,
           taxon.model.hierarchical,
           IlluminateAlgorithm.projectAttributes(taxon.form.attributes, taxon.shadow.attributes),
           TermResponse.buildTree(terms))
    }

    case class Root(id: Int, hierarchical: Boolean, attributes: Json, terms: TermList)
  }

  object TermResponse {
    case class Root(id: Int, attributes: Json, children: TermList)

    def build(term: FullObject[Term]): Root = {
      Root(term.model.formId,
           IlluminateAlgorithm.projectAttributes(term.form.attributes, term.shadow.attributes),
           Seq())
    }

    def buildTree(nodes: Seq[LinkedTerm]): TermList =
      buildTree(0, nodes.sortBy { case (_, link) ⇒ link.path.level })

    private def buildTree(level: Int, nodesSorted: Seq[LinkedTerm]): TermList = {
      val (heads, tail)   = nodesSorted.span { case (_, link) ⇒ link.path.level == level }
      val headsByPosition = heads.sortBy { case (_, link)     ⇒ link.position }
      headsByPosition.map {
        case (term, link) ⇒
          Root(term.model.formId,
               IlluminateAlgorithm.projectAttributes(term.form.attributes, term.shadow.attributes),
               buildTree(level + 1, tail.filter {
                 case (_, lnk) ⇒ lnk.parentIndex.contains(link.index)
               }))
      }
    }
  }
}
