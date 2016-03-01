package payloads

import models.activity.Aliases.Json

final case class CreateProductForm(id: Int, attributes: Json, variants: Json)
final case class CreateProductShadow(productId: Int, attributes: Json)
final case class CreateProductContext(name: String, attributes: Json)
final case class UpdateProductForm(id: Int, attributes: Json, variants: Json, isActive: Boolean)
final case class UpdateProductShadow(productId: Int, attributes: Json)
final case class UpdateProductContext(name: String, attributes: Json)
