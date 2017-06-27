package phoenix.services.activity

import objectframework.ObjectResponses.ObjectContextResponse
import phoenix.responses.CategoryResponses.FullCategoryResponse
import phoenix.responses.users.UserResponse

object CategoryTailored {
  case class FullCategoryCreated(admin: Option[UserResponse],
                                 category: FullCategoryResponse,
                                 context: ObjectContextResponse)
      extends ActivityBase[FullCategoryCreated]

  case class FullCategoryUpdated(admin: Option[UserResponse],
                                 category: FullCategoryResponse,
                                 context: ObjectContextResponse)
      extends ActivityBase[FullCategoryUpdated]
}
