package services.activity

import responses.ObjectResponses.ObjectContextResponse
import responses.CategoryResponses.FullCategoryResponse
import responses.StoreAdminResponse

object CategoryTailored {
  case class FullCategoryCreated(admin: Option[StoreAdminResponse.Root], category: FullCategoryResponse.Root,
    context: ObjectContextResponse.Root) extends ActivityBase[FullCategoryCreated]

  case class FullCategoryUpdated(admin: Option[StoreAdminResponse.Root], category: FullCategoryResponse.Root,
    context: ObjectContextResponse.Root) extends ActivityBase[FullCategoryUpdated]
}
