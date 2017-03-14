package services.activity

import responses.UserResponse
import responses.ProductResponses.ProductResponse
import responses.ObjectResponses.ObjectContextResponse

object ProductTailored {
  case class FullProductCreated(admin: Option[UserResponse.Root],
                                product: ProductResponse.Root,
                                context: ObjectContextResponse.Root)
      extends ActivityBase[FullProductCreated]

  case class FullProductUpdated(admin: Option[UserResponse.Root],
                                product: ProductResponse.Root,
                                context: ObjectContextResponse.Root)
      extends ActivityBase[FullProductUpdated]

  case class FullProductArchived(admin: Option[UserResponse.Root],
                                 product: ProductResponse.Root,
                                 context: ObjectContextResponse.Root)
      extends ActivityBase[FullProductArchived]
}
