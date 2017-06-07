package phoenix.responses

import java.time.Instant

import cats.implicits._
import objectframework.ObjectResponses.ObjectContextResponse
import objectframework.models._
import phoenix.models.category._
import phoenix.utils.aliases._

object CategoryResponses {

  object CategoryHeadResponse {

    case class Root(id: Int) extends ResponseItem

    def build(c: Category): Root = Root(c.formId)
  }

  object CategoryFormResponse {

    case class Root(id: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(c: Category, f: ObjectForm): Root = Root(f.id, f.attributes, c.createdAt)
  }

  object CategoryShadowResponse {

    case class Root(id: Int, formId: Int, attributes: Json, createdAt: Instant) extends ResponseItem

    def build(c: ObjectShadow): Root = Root(c.id, c.formId, c.attributes, c.createdAt)
  }

  object IlluminatedCategoryResponse {

    case class Root(id: Int, context: Option[ObjectContextResponse.Root], attributes: Json)
        extends ResponseItem

    def build(c: IlluminatedCategory): Root =
      Root(c.id, ObjectContextResponse.build(c.context).some, c.attributes)
  }

  object FullCategoryResponse {

    case class Root(form: CategoryFormResponse.Root, shadow: CategoryShadowResponse.Root) extends ResponseItem

    def build(category: Category, categoryForm: ObjectForm, categoryShadow: ObjectShadow): Root =
      Root(CategoryFormResponse.build(category, categoryForm), CategoryShadowResponse.build(categoryShadow))
  }
}
