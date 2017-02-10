package services.activity

import responses.ObjectResponses.ObjectContextResponse
import responses.ProductVariantResponses.ProductVariantResponse
import responses.UserResponse

object ProductVariantTailored {
  case class FullVariantCreated(admin: Option[UserResponse.Root],
                                variant: ProductVariantResponse.Root,
                                context: ObjectContextResponse.Root)
      extends ActivityBase[FullVariantCreated]

  case class FullVariantUpdated(admin: Option[UserResponse.Root],
                                variant: ProductVariantResponse.Root,
                                context: ObjectContextResponse.Root)
      extends ActivityBase[FullVariantUpdated]
}
