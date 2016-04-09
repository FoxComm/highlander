package failures

import concepts.discounts.offers.OfferType
import concepts.discounts.qualifiers.QualifierType.show
import concepts.discounts.offers.{Offer, OfferType}
import concepts.discounts.qualifiers.{Qualifier, QualifierType}
import utils.friendlyClassName

object DiscountCompilerFailures {

  /* Qualifier AST compiler */
  final case class QualifierAstParseFailure(json: String) extends Failure {
    override def description = s"failed to compile qualifiers AST, invalid JSON provided: $json"
  }

  case object QualifierAstInvalidFormatFailure extends Failure {
    override def description = s"failed to compile qualifiers AST, invalid format provided"
  }

  case object QualifierAstEmptyObjectFailure extends Failure {
    override def description = s"failed to compile qualifiers AST, no qualifiers found inside payload"
  }

  /* Offer AST compiler */
  final case class OfferAstParseFailure(json: String) extends Failure {
    override def description = s"failed to compile offers AST, invalid JSON provided: $json"
  }

  case object OfferAstInvalidFormatFailure extends Failure {
    override def description = s"failed to compile offers AST, invalid format provided"
  }

  case object OfferAstEmptyObjectFailure extends Failure {
    override def description = s"failed to compile offers AST, no offers found inside payload"
  }

  /* Qualifier Compiler */
  final case class QualifierAttributesExtractionFailure(qualifierType: QualifierType) extends Failure {
    override def description = s"failed to compile qualifier ${show(qualifierType)}, couldn't extract attributes"
  }

  final case class QualifierNotImplementedFailure(qualifierType: QualifierType) extends Failure {
    override def description = s"qualifier not implemented for qualifier type ${show(qualifierType)}"
  }

  /* Offer Compiler */
  final case class OfferAttributesExtractionFailure(offerType: OfferType) extends Failure {
    override def description = s"failed to compile offer ${OfferType.show(offerType)}, couldn't extract attributes"
  }

  final case class OfferNotImplementedFailure(offerType: OfferType) extends Failure {
    override def description = s"offer not implemented for offer type ${OfferType.show(offerType)}"
  }

  /* Rejections */
  final case class QualifierRejectionFailure[T <: Qualifier](qualifier: T, refNum: String, reason: String) extends Failure {
    override def description = s"qualifier ${friendlyClassName(qualifier)} rejected order with refNum=$refNum, reason: $reason"
  }

  final case class OfferRejectionFailure[T <: Offer](offer: T, refNum: String, reason: String) extends Failure {
    override def description = s"offer ${friendlyClassName(offer)} rejected order with refNum=$refNum, reason: $reason"
  }
}
