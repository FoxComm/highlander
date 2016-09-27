package failures

import models.objects.ObjectForm
import models.taxonomy._

object TaxonomyFailures {

  case class NoChildrenForFlatTaxonomy(taxonId: Int) extends Failure {
    override def description: String = s"Taxon with id=$taxonId is flat. Cannot add child term."
  }

  case object NoTermInTaxonomy {
    def apply(taxon: Taxon, term: Term): Failure = apply(taxon.formId, term.formId)

    def apply(taxonFormId: ObjectForm#Id, termFormId: ObjectForm#Id): Failure = new Failure {
      override def description: String =
        s"Taxon with id=$taxonFormId does not contain term with id=$termFormId"
    }
  }

  case class InvalidTermTaxons(term: Term, taxonsCount: Int) extends Failure {
    override def description: String =
      if (taxonsCount == 0) s"Term with id = ${term.id} is not linked to any taxon"
      else s"Term with id = ${term.id} is linked to multiple taxons"
  }
}
