package services.taxonomy

import cats.data._
import failures.Failure
import failures.TaxonomyFailures.ParentOrSiblingIsInvalid
import utils.Validation

object TaxonomyValidation {

  def validateParentOrSiblingIsDefined(parent: Option[Int],
                                       sibling: Option[Int]): ValidatedNel[Failure, Unit] =
    Validation.validExpr(sibling.isEmpty || sibling.isDefined && parent.isEmpty,
                         ParentOrSiblingIsInvalid.description)

}
