package phoenix.responses

import java.time.Instant

import cats.implicits._
import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.category._
import phoenix.utils.aliases._

object CategoryResponses {

  case class CategoryHeadResponse(id: Int) extends ResponseItem

  object CategoryHeadResponse {
    def build(c: Category): CategoryHeadResponse = CategoryHeadResponse(c.formId)
  }

  case class CategoryFormResponse(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

  object CategoryFormResponse {
    def build(c: Category, f: ObjectForm): CategoryFormResponse =
      CategoryFormResponse(f.id, f.attributes, c.createdAt)
  }

  case class CategoryShadowResponse(id: Int, formId: Int, attributes: Json, createdAt: Instant)
      extends ResponseItem

  object CategoryShadowResponse {
    def build(c: ObjectShadow): CategoryShadowResponse =
      CategoryShadowResponse(c.id, c.formId, c.attributes, c.createdAt)
  }

  case class IlluminatedCategoryResponse(id: Int, context: Option[ObjectContextResponse], attributes: Json)
      extends ResponseItem

  object IlluminatedCategoryResponse {
    def build(c: IlluminatedCategory): IlluminatedCategoryResponse =
      IlluminatedCategoryResponse(c.id, ObjectContextResponse.build(c.context).some, c.attributes)
  }

  case class FullCategoryResponse(form: CategoryFormResponse, shadow: CategoryShadowResponse)
      extends ResponseItem

  object FullCategoryResponse {
    def build(category: Category,
              categoryForm: ObjectForm,
              categoryShadow: ObjectShadow): FullCategoryResponse =
      FullCategoryResponse(CategoryFormResponse.build(category, categoryForm),
                           CategoryShadowResponse.build(categoryShadow))
  }
}
