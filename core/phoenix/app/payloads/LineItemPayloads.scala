package payloads

import cats.data._
import failures.Failure
import utils.Money._
import utils.Validation
import utils.Validation._

object LineItemPayloads {

  case class UpdateLineItemsPayload(sku: String, quantity: Int)
}
