package phoenix.services.activity

import objectframework.ObjectResponses.ObjectContextResponse
import phoenix.responses.ProductResponses.ProductResponse
import phoenix.responses.users.UserResponse

object ProductTailored {
  case class FullProductCreated(admin: Option[UserResponse],
                                product: ProductResponse,
                                context: ObjectContextResponse)
      extends ActivityBase[FullProductCreated]

  case class FullProductUpdated(admin: Option[UserResponse],
                                product: ProductResponse,
                                context: ObjectContextResponse)
      extends ActivityBase[FullProductUpdated]
}
