package payloads

import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive
import utils.aliases._

object CouponPayloads {

  case class CreateCoupon(attributes: Map[String, Json],
                          promotion: Int,
                          schema: Option[String] = None,
                          scope: Option[String] = None)
  case class UpdateCoupon(attributes: Map[String, Json], promotion: Int)

  case class GenerateCouponCodes(prefix: String Refined NonEmpty,
                                 quantity: Int Refined Positive,
                                 length: Int Refined Positive)
}
