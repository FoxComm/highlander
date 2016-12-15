package services.activity

import responses.ObjectResponses.ObjectContextResponse
import responses.ProductVariantResponses.ProductVariantResponse
import responses.UserResponse

object SkuTailored {
  case class FullSkuCreated(admin: Option[UserResponse.Root],
                            sku: ProductVariantResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuCreated]

  case class FullSkuUpdated(admin: Option[UserResponse.Root],
                            sku: ProductVariantResponse.Root,
                            context: ObjectContextResponse.Root)
      extends ActivityBase[FullSkuUpdated]
}
