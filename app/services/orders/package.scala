package services

import responses.{AllOrders, TheResponse}

package object orders {
  type BulkOrderUpdateResponse = TheResponse[Seq[AllOrders.Root]]
}
