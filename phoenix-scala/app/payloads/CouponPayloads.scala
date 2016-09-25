package payloads

import utils.aliases._

object CouponPayloads {

  case class CreateCoupon(attributes: Map[String, Json], promotion: Int)
  case class UpdateCoupon(attributes: Map[String, Json], promotion: Int)

  case class GenerateCouponCodes(prefix: String, quantity: Int, length: Int)
}
