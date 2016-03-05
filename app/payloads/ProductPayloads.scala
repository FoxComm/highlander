package payloads

import models.Aliases.Json
import java.time.Instant

final case class CreateProductForm(attributes: Json, variants: Json)
final case class CreateProductShadow(attributes: Json, variants: String,
  activeFrom: Option[Instant], activeTo: Option[Instant])
final case class CreateProductContext(name: String, attributes: Json)
final case class UpdateProductForm(attributes: Json, variants: Json)
final case class UpdateProductShadow(attributes: Json, variants: String, 
  activeFrom: Option[Instant], activeTo: Option[Instant])
final case class UpdateProductContext(name: String, attributes: Json)

final case class CreateFullProductForm(product: CreateProductForm, skus: Seq[CreateFullSkuForm])
final case class UpdateFullProductForm(product: UpdateProductForm, skus: Seq[UpdateFullSkuForm])
final case class CreateFullProductShadow(product: CreateProductShadow, skus: Seq[CreateSkuShadow])
final case class UpdateFullProductShadow(product: UpdateProductShadow, skus: Seq[UpdateFullSkuShadow])

final case class CreateFullProduct(form: CreateFullProductForm, shadow: CreateFullProductShadow)
final case class UpdateFullProduct(form: UpdateFullProductForm, shadow: UpdateFullProductShadow)
