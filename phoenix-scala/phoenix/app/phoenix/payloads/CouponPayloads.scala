package phoenix.payloads

import phoenix.utils.aliases._

object CouponPayloads {

  case class CreateCoupon(attributes: Map[String, Json],
                          promotion: Int,
                          schema: Option[String] = None,
                          scope: Option[String] = None,
                          singleCode: Option[String],
                          generateCodes: Option[GenerateCouponCodes])

  // FIXME: unused? @michalrus
  //case class UpdateCoupon(attributes: Map[String, Json], promotion: Int)

  case class GenerateCouponCodes(prefix: String, quantity: Int, length: Int)
}
