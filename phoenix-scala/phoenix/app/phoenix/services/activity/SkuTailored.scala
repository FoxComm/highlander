package phoenix.services.activity

import objectframework.ObjectResponses.ObjectContextResponse
import phoenix.responses.SkuResponses.SkuResponse
import phoenix.responses.users.UserResponse

object SkuTailored {
  case class FullSkuCreated(admin: Option[UserResponse], sku: SkuResponse, context: ObjectContextResponse)
      extends ActivityBase[FullSkuCreated]

  case class FullSkuUpdated(admin: Option[UserResponse], sku: SkuResponse, context: ObjectContextResponse)
      extends ActivityBase[FullSkuUpdated]
}
