package services.activity

import responses.ObjectResponses.ObjectContextResponse
import responses.ProductVariantResponses.ProductVariantResponse
import responses.UserResponse

object ProductVariantTailored {
  case class FullProductVariantCreated(admin: Option[UserResponse.Root],
                                       sku: ProductVariantResponse.Root,
                                       context: ObjectContextResponse.Root)
      extends ActivityBase[FullProductVariantCreated]

  case class FullProductVariantUpdated(admin: Option[UserResponse.Root],
                                       sku: ProductVariantResponse.Root,
                                       context: ObjectContextResponse.Root)
      extends ActivityBase[FullProductVariantUpdated]
}
