package services.activity

import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses.SkuResponse
import responses.UserResponse

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
