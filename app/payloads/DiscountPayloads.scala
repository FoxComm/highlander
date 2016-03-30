package payloads

import models.Aliases.Json
import java.time.Instant

final case class CreateDiscountForm(attributes: Json)
final case class CreateDiscountShadow(attributes: Json)
final case class UpdateDiscountForm(attributes: Json)
final case class UpdateDiscountShadow(attributes: Json)
