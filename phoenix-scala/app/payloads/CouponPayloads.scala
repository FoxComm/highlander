package payloads

import utils.aliases._

object CouponPayloads {

  case class CreateCouponForm(attributes: Json)

  case class CreateCouponShadow(attributes: Json)

  case class CreateCoupon(form: CreateCouponForm,
                          shadow: CreateCouponShadow,
                          promotion: Int,
                          scope: Option[String] = None)

  case class UpdateCouponForm(attributes: Json)

  case class UpdateCouponShadow(attributes: Json)

  case class UpdateCoupon(form: UpdateCouponForm, shadow: UpdateCouponShadow, promotion: Int)

  case class GenerateCouponCodes(prefix: String, quantity: Int, length: Int)
}
