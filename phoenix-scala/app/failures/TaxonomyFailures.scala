package failures

import models.objects.ObjectForm
import models.taxonomy._

object TaxonomyFailures {

  case class NoChildrenForFlatTaxonomy(taxonomyId: ObjectForm#Id) extends Failure {
    override def description: String =
      s"Taxonomy with id=$taxonomyId is flat. Cannot add child taxon."
  }

  case object NoTermInTaxonomy {
    def apply(taxon: Taxonomy, term: Taxon): Failure = apply(taxon.formId, term.formId)

    def apply(taxonomyFormId: ObjectForm#Id, taxonFormId: ObjectForm#Id): Failure = new Failure {
      override def description: String =
        s"Taxonomy with id=$taxonomyFormId does not contain taxon with id=$taxonFormId"
    }
  }

  case class InvalidTaxonomiesForTaxon(taxon: Taxon, taxonsCount: Int) extends Failure {
    override def description: String =
      if (taxonsCount == 0) s"Taxon with id = ${taxon.id} is not linked to any taxonomy"
      else s"Taxon with id = ${taxon.id} is linked to multiple taxonomies"
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

  case object CannotMoveParentTaxonUnderChild extends Failure {
    override def description: String = "cannot move parent taxon under itself or one of its child"
  }

  case class CannotUnassignProduct(taxonId: Taxon#Id, productId: models.product.Product#Id)
      extends Failure {
    override def description: String =
      s"Cannot delete taxon-product link. TaxonId:$taxonId, productId: $productId"
  }

  case class NoTaxonAtPosition(parent: Option[ObjectForm#Id], position: Int) extends Failure {
    override def description: String = parent match {
      case Some(parentId) ⇒ s"Taxon $parentId has no child at position before $position"
      case _              ⇒ s"Invalid position value $position"
    }
  }

  case object TaxonomyShouldMatchForParentAndTarget extends Failure {
    override def description: String = "taxon should belong to the same taxonomy as parent one"
  }
}
