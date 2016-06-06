package payloads

import utils.aliases._

object SkuPayloads {

  case class CreateSkuForm(code: String, productId: Int, attributes: Json)

  case class CreateSkuShadow(code: String, attributes: Json)

  case class UpdateSkuForm(attributes: Json)

  case class UpdateSkuShadow(attributes: Json)

  case class CreateFullSkuForm(code: String, attributes: Json)

  case class CreateFullSkuShadow(code: String, attributes: Json)

  case class UpdateFullSkuForm(code: String, attributes: Json)

  case class UpdateFullSkuShadow(code: String, attributes: Json)

  case class CreateFullSku(form: CreateFullSkuForm, shadow: CreateFullSkuShadow)

  case class UpdateFullSku(form: UpdateFullSkuForm, shadow: UpdateFullSkuShadow)

  // New payloads
  case class CreateSkuPayload(code: String, attributes: Map[String, Json])
}
