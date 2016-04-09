package payloads

import models.Aliases.Json
import java.time.Instant

final case class CreateCouponForm(attributes: Json)
final case class CreateCouponShadow(attributes: Json)
final case class CreateCoupon(form: CreateCouponForm, shadow: CreateCouponShadow, promotion: Int)
final case class UpdateCouponForm(attributes: Json)
final case class UpdateCouponShadow(attributes: Json)
final case class UpdateCoupon(form: UpdateCouponForm, shadow: UpdateCouponShadow, promotion: Int)
final case class GenerateCouponCodes(prefix: String, quantity: Int, length: Int)
