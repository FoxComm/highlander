package failures

import models.discount.DiscountInput
import models.discount.offers.{Offer, OfferType}
import models.discount.qualifiers.QualifierType.show
import models.discount.qualifiers.{Qualifier, QualifierType}
import utils.friendlyClassName

object DiscountCompilerFailures {

  /* General errors */
  case object EmptyDiscountFailure extends Failure {
    override def description = "Promotion is missing discount object"
  }

  case object EmptyOfferFailure extends Failure {
    override def description = "Discount is missing offer object"
  }

  case object EmptyQualifierFailure extends Failure {
    override def description = "Discount is missing qualifier object"
  }

  /* Qualifier AST compiler */
  case class QualifierAstParseFailure(json: String) extends Failure {
    override def description = s"failed to compile qualifiers AST, invalid JSON provided: $json"
  }

  case object QualifierAstInvalidFormatFailure extends Failure {
    override def description = "failed to compile qualifiers AST, invalid format provided"
  }

  /* Offer AST compiler */
  case class OfferAstParseFailure(json: String) extends Failure {
    override def description = s"failed to compile offers AST, invalid JSON provided: $json"
  }

  case class OfferNotValid(offer: String) extends Failure {
    override def description = s"failed to compile offers AST, invalid offer: $offer"
  }

  case object OfferAstInvalidFormatFailure extends Failure {
    override def description = "failed to compile offers AST, invalid format provided"
  }

  /* Qualifier Compiler */
  case class QualifierAttributesExtractionFailure(qualifierType: QualifierType) extends Failure {
    override def description =
      s"failed to compile qualifier ${show(qualifierType)}, couldn't extract attributes"
  }

  case class QualifierNotImplementedFailure(qualifierType: QualifierType) extends Failure {
    override def description =
      s"qualifier not implemented for qualifier type ${show(qualifierType)}"
  }

  case class QualifierNotValid(qualifier: String) extends Failure {
    override def description = s"qualifier $qualifier is not valid"
  }

  case class QualifierSearchIsEmpty(qualifierType: QualifierType) extends Failure {
    override def description =
      s"failed to compile qualifier ${show(qualifierType)}, search attribute is missing"
  }

  /* Offer Compiler */
  case class OfferAttributesExtractionFailure(offerType: OfferType) extends Failure {
    override def description =
      s"failed to compile offer ${OfferType.show(offerType)}, couldn't extract attributes"
  }

  case class OfferNotImplementedFailure(offerType: OfferType) extends Failure {
    override def description = s"offer not implemented for offer type ${OfferType.show(offerType)}"
  }

  case class OfferSearchIsEmpty(offerType: OfferType) extends Failure {
    override def description =
      s"failed to compile offer ${OfferType.show(offerType)}, search attribute is missing"
  }

  /* Rejections */
  case class QualifierRejectionFailure[T <: Qualifier](qualifier: T,
                                                       input: DiscountInput,
                                                       reason: String)
      extends Failure {
    val qName = friendlyClassName(qualifier)
    override def description =
      s"qualifier $qName rejected order with refNum=${input.cart.refNum}, reason: $reason"
  }

  case class OfferRejectionFailure[T <: Offer](offer: T, input: DiscountInput, reason: String)
      extends Failure {
    val oName = friendlyClassName(offer)
    override def description =
      s"offer $oName rejected order with refNum=${input.cart.refNum}, reason: $reason"
  }
}
