package payloads

import models.Aliases.Json

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

}
