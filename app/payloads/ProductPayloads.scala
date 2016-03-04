package payloads

import models.Aliases.Json

final case class CreateProductForm(attributes: Json, variants: Json)
final case class CreateProductShadow(attributes: Json)
final case class CreateProductContext(name: String, attributes: Json)
final case class UpdateProductForm(attributes: Json, variants: Json, isActive: Boolean)
final case class UpdateProductShadow(attributes: Json)
final case class UpdateProductContext(name: String, attributes: Json)

final case class CreateFullProductForm(product: CreateProductForm, skus:  Seq[CreateSkuForm])
final case class UpdateFullProductForm(product: UpdateProductForm, skus:  Seq[UpdateFullSkuForm])
final case class CreateFullProductShadow(product: CreateProductShadow, skus:  Seq[CreateSkuShadow])
final case class UpdateFullProductShadow(product: UpdateProductShadow, skus:  Seq[UpdateFullSkuShadow])

final case class CreateFullProduct(form: CreateFullProductForm, shadow: CreateFullProductShadow)
final case class UpdateFullProduct(form: UpdateFullProductForm, shadow: UpdateFullProductShadow)
