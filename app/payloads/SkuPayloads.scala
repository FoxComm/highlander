package payloads

import models.Aliases.Json

final case class CreateSkuForm(code: String, productId: Int, attributes: Json, isActive: Boolean, isHazardous: Boolean)
final case class CreateFullSkuForm(code: String, attributes: Json, isActive: Boolean, isHazardous: Boolean)
final case class CreateSkuShadow(code: String, attributes: Json)
final case class UpdateSkuForm(attributes: Json, isActive: Boolean, isHazardous: Boolean)
final case class UpdateFullSkuForm(code: String, attributes: Json, isActive: Boolean, isHazardous: Boolean)
final case class UpdateSkuShadow(attributes: Json)
final case class UpdateFullSkuShadow(code: String, attributes: Json)
