package services.activity

import responses.StoreAdminResponse
import responses.ProductResponses.FullProductResponse
import responses.ObjectResponses.ObjectContextResponse

object ProductTailored {
  case class FullProductCreated(admin: Option[StoreAdminResponse.Root],
                                product: FullProductResponse.Root,
                                context: ObjectContextResponse.Root)
      extends ActivityBase[FullProductCreated]

  case class FullProductUpdated(admin: Option[StoreAdminResponse.Root],
                                product: FullProductResponse.Root,
                                context: ObjectContextResponse.Root)
      extends ActivityBase[FullProductUpdated]
}
