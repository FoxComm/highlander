package phoenix.services.activity

import responses.ObjectResponses.ObjectContextResponse
import phoenix.responses.CategoryResponses.FullCategoryResponse
import phoenix.responses.UserResponse

object CategoryTailored {
  case class FullCategoryCreated(admin: Option[UserResponse.Root],
                                 category: FullCategoryResponse.Root,
                                 context: ObjectContextResponse.Root)
      extends ActivityBase[FullCategoryCreated]

  case class FullCategoryUpdated(admin: Option[UserResponse.Root],
                                 category: FullCategoryResponse.Root,
                                 context: ObjectContextResponse.Root)
      extends ActivityBase[FullCategoryUpdated]
}
