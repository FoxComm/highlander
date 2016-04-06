package services.activity

import responses.StoreAdminResponse
import responses.ProductResponses.FullProductResponse

object ProductTailored {
  final case class ProductCreated(admin: Option[StoreAdminResponse.Root], product: FullProductResponse.Root)
    extends ActivityBase[ProductCreated]
}