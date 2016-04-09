package payloads

import models.Aliases.Json
import java.time.Instant

final case class CreateDiscountForm(attributes: Json)
final case class CreateDiscountShadow(attributes: Json)
final case class CreateDiscount(form: CreateDiscountForm, shadow: CreateDiscountShadow)
final case class UpdateDiscountForm(attributes: Json)
final case class UpdateDiscountShadow(attributes: Json)
final case class UpdateDiscount(form: UpdateDiscountForm, shadow: UpdateDiscountShadow)
