package payloads

import models.Aliases.Json

case class CreateCategoryForm(attributes: Json)
case class CreateCategoryShadow(attributes: Json)
case class UpdateCategoryForm(attributes: Json)
case class UpdateCategoryShadow(attributes: Json)

case class CreateFullCategory(form: CreateCategoryForm, shadow: CreateCategoryShadow)
case class UpdateFullCategory(form: UpdateCategoryForm, shadow: UpdateCategoryShadow)
