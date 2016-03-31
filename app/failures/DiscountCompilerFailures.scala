package failures

import concepts.discounts._
import utils.friendlyClassName

object DiscountCompilerFailures {

  final case class QualifierRejectionFailure[T <: Qualifier](qualifier: T, refNum: String, reason: String) extends Failure {
    override def description = s"qualifier ${friendlyClassName(qualifier)} rejected order with refNum=$refNum, reason: $reason"
  }

  final case class OfferRejectionFailure[T <: Offer](offer: T, refNum: String, reason: String) extends Failure {
    override def description = s"offer ${friendlyClassName(offer)} rejected order with refNum=$refNum, reason: $reason"
  }

  final case class QualifierAttributesParseFailure(qualifierType: String, json: String) extends Failure {
    override def description = s"failed to compile qualifier $qualifierType, invalid JSON provided: $json"
  }

  final case class QualifierAttributesExtractionFailure(qualifierType: String, json: String) extends Failure {
    override def description = s"failed to compile qualifier $qualifierType, couldn't extract attributes from $json"
  }

  final case class UnknownQualifierFailure(qualifierType: String) extends Failure {
    override def description = s"unknown qualifier type $qualifierType"
  }

  final case class QualifierNotImplementedFailure(qualifierType: String) extends Failure {
    override def description = s"qualifier not implemented for qualifier type $qualifierType"
  }

  final case class OfferAttributesParseFailure(offerType: String, json: String) extends Failure {
    override def description = s"failed to compile offer $offerType, invalid JSON provided: $json"
  }

  final case class OfferAttributesExtractionFailure(offerType: String, json: String) extends Failure {
    override def description = s"failed to compile offer $offerType, couldn't extract attributes from $json"
  }

  final case class UnknownOfferFailure(offerType: String) extends Failure {
    override def description = s"unknown offer type $offerType"
  }

  final case class OfferNotImplementedFailure(offerType: String) extends Failure {
    override def description = s"offer not implemented for offer type $offerType"
  }
}
