package payloads

import payloads.SkuPayloads._
import utils.aliases._

object ProductPayloads {

  case class CreateProductForm(attributes: Json)

  case class CreateProductShadow(attributes: Json)

  case class UpdateProductForm(attributes: Json)

  case class UpdateProductShadow(attributes: Json)

  case class CreateFullProductForm(product: CreateProductForm, skus: Seq[CreateFullSkuForm])

  case class UpdateFullProductForm(product: UpdateProductForm, skus: Seq[UpdateFullSkuForm])

  case class CreateFullProductShadow(product: CreateProductShadow, skus: Seq[CreateSkuShadow])

  case class UpdateFullProductShadow(product: UpdateProductShadow, skus: Seq[UpdateFullSkuShadow])

  case class CreateFullProduct(form: CreateFullProductForm, shadow: CreateFullProductShadow)

  case class UpdateFullProduct(form: UpdateFullProductForm, shadow: UpdateFullProductShadow)
}
