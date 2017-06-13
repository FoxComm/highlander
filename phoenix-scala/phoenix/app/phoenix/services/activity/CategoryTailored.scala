package phoenix.services.activity

import objectframework.ObjectResponses.ObjectContextResponse
import phoenix.responses.CategoryResponses.FullCategoryResponse
import phoenix.responses.users.UserResponse

object CategoryTailored {
  case class FullCategoryCreated(admin: Option[UserResponse],
                                 category: FullCategoryResponse.Root,
                                 context: ObjectContextResponse.Root)
      extends ActivityBase[FullCategoryCreated]

  case class FullCategoryUpdated(admin: Option[UserResponse],
                                 category: FullCategoryResponse.Root,
                                 context: ObjectContextResponse.Root)
      extends ActivityBase[FullCategoryUpdated]
}
