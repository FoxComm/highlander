package services.activity

import responses.StoreAdminResponse
import responses.ProductResponses.{FullProductResponse, ObjectContextResponse}

object ProductTailored {
  final case class FullProductCreated(admin: Option[StoreAdminResponse.Root], product: FullProductResponse.Root,
    context: ObjectContextResponse.Root) extends ActivityBase[FullProductCreated]

  final case class FullProductUpdated(admin: Option[StoreAdminResponse.Root], product: FullProductResponse.Root,
    context: ObjectContextResponse.Root) extends ActivityBase[FullProductUpdated]
}