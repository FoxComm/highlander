package payloads

import models.Aliases.Json
import java.time.Instant

final case class CreateSkuForm(code: String, productId: Int, attributes: Json)
final case class CreateFullSkuForm(code: String, attributes: Json)
final case class CreateSkuShadow(code: String, attributes: Json)
final case class UpdateSkuForm(attributes: Json)
final case class UpdateFullSkuForm(code: String, attributes: Json)
final case class UpdateSkuShadow(attributes: Json)
final case class UpdateFullSkuShadow(code: String, attributes: Json)
