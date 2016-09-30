package failures

import models.objects.ObjectForm
import models.taxonomy._

object TaxonomyFailures {

  case class NoChildrenForFlatTaxonomy(taxonId: Int) extends Failure {
    override def description: String = s"Taxon with id=$taxonId is flat. Cannot add child term."
  }

  case object NoTermInTaxonomy {
    def apply(taxon: Taxonomy, term: Taxon): Failure = apply(taxon.formId, term.formId)

    def apply(taxonFormId: ObjectForm#Id, termFormId: ObjectForm#Id): Failure = new Failure {
      override def description: String =
        s"Taxon with id=$taxonFormId does not contain term with id=$termFormId"
    }
  }

  case class InvalidTaxonomiesForTaxon(term: Taxon, taxonsCount: Int) extends Failure {
    override def description: String =
      if (taxonsCount == 0) s"Term with id = ${term.id} is not linked to any taxon"
      else s"Term with id = ${term.id} is linked to multiple taxons"
  }

  case class TaxonIsArchived(taxonId: ObjectForm#Id) extends Failure {
    override def description: String = s"Cannot update deleted taxon: $taxonId"
  }

  case class TaxonomyIsArchived(taxonomyId: ObjectForm#Id) extends Failure {
    override def description: String = s"Cannot update deleted taxonomy: $taxonomyId"
  }

  case class CannotArchiveParentTaxon(taxonId: ObjectForm#Id) extends Failure {
    override def description: String = s"Cannot archive taxon $taxonId as soon as it has child"
  }

  case object ParentOrSiblingIsInvalid extends Failure {
    override def description: String = "'parent' should be empty if 'sibling' is defined"
  }

  case object CannotMoveParentTaxonUnderChild extends Failure {
    override def description: String = "cannot move parent taxon under itself or one of its child"
  }
}
