package payloads

import utils.aliases._

object CategoryPayloads {

  case class CreateCategoryForm(attributes: Json)

  case class CreateCategoryShadow(attributes: Json)

  case class UpdateCategoryForm(attributes: Json)

  case class UpdateCategoryShadow(attributes: Json)

  case class CreateFullCategory(form: CreateCategoryForm,
                                shadow: CreateCategoryShadow,
                                schema: Option[String] = None,
                                scope: Option[String] = None)

  case class UpdateFullCategory(form: UpdateCategoryForm, shadow: UpdateCategoryShadow)
}
