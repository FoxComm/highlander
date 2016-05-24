package services.activity

import responses.ObjectResponses.ObjectContextResponse
import responses.SkuResponses.FullSkuResponse
import responses.StoreAdminResponse

object SkuTailored {
  case class FullSkuCreated(admin: Option[StoreAdminResponse.Root],
                            sku: FullSkuResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuCreated]

  case class FullSkuUpdated(admin: Option[StoreAdminResponse.Root],
                            sku: FullSkuResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuUpdated]
}
