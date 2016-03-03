package payloads

import models.Aliases.Json

final case class CreateSkuForm(code: String, productId: Int, attributes: Json, isActive: Boolean, isHazardous: Boolean)
final case class CreateSkuShadow(code: String, attributes: Json)
final case class UpdateSkuForm(attributes: Json, isActive: Boolean, isHazardous: Boolean)
final case class UpdateSkuShadow(attributes: Json)
