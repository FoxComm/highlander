package phoenix.services.activity

import phoenix.responses.UserResponse
import phoenix.responses.ProductResponses.ProductResponse
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
}
