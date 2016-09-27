package responses

import models.objects.FullObject
import models.taxonomy._
import utils.IlluminateAlgorithm
import utils.aliases.Json

object TaxonomyResponses {
  type TermList   = Seq[TermResponse.Root]
  type LinkedTerm = (FullObject[Term], TaxonTermLink)

  object TaxonResponse {

    def build(taxon: FullObject[Taxon], terms: Seq[LinkedTerm]): Root = {
      Root(taxon.model.formId,
           IlluminateAlgorithm.projectAttributes(taxon.form.attributes, taxon.shadow.attributes),
           TermResponse.buildTree(terms))
    }

    case class Root(id: Int, attributes: Json, terms: TermList)
  }

  object TermResponse {
    case class Root(id: Int, attributes: Json, children: TermList)

    def build(term: FullObject[Term]): Root = {
      Root(term.model.formId,
           IlluminateAlgorithm.projectAttributes(term.form.attributes, term.shadow.attributes),
           Seq())
    }

    def buildTree(nodes: Seq[LinkedTerm]): TermList =
      buildTree(0, nodes.sortBy { case (_, link) ⇒ link.path.value.size })

    private def buildTree(level: Int, nodesSorted: Seq[LinkedTerm]): TermList = {
      val (heads, tail) = nodesSorted.span { case (_, link) ⇒ link.path.value.size == level }
      heads.map {
        case (term, link) ⇒
          Root(term.model.formId,
               IlluminateAlgorithm.projectAttributes(term.form.attributes, term.shadow.attributes),
               buildTree(level + 1, tail.filter {
                 case (_, lnk) ⇒ lnk.path.value.startsWith(link.path.value)
               }))
      }
    }
  }
}
