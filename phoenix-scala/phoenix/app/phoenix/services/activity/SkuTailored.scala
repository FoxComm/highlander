package phoenix.services.activity

import responses.ObjectResponses.ObjectContextResponse
import phoenix.responses.SkuResponses.SkuResponse
import phoenix.responses.UserResponse

object SkuTailored {
  case class FullSkuCreated(admin: Option[UserResponse.Root],
                            sku: SkuResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuCreated]

  case class FullSkuUpdated(admin: Option[UserResponse.Root],
                            sku: SkuResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuUpdated]
}
