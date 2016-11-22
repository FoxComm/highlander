package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import models.cord.lineitems.OrderLineItem
import utils.Money._
import utils.Validation
import utils.Validation._
import utils.aliases._

object LineItemPayloads {

  case class UpdateLineItemsPayload(sku: String, quantity: Int, attributes: Option[Json] = None)
  case class UpdateOrderLineItemsPayload(previousAttributes: Option[Json] = None,
                                         state: OrderLineItem.State,
                                         attributes: Option[Json],
                                         referenceNumber: String,
                                         sku: String)
}
