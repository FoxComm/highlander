package services.activity

import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses.SkuResponse
import responses.StoreAdminResponse

object SkuTailored {
  case class FullSkuCreated(admin: Option[StoreAdminResponse.Root],
                            sku: SkuResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuCreated]

  case class FullSkuUpdated(admin: Option[StoreAdminResponse.Root],
                            sku: SkuResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuUpdated]
}
