package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import utils.Money._
import utils.Validation
import utils.Validation._
import utils.aliases.Json

object LineItemPayloads {

  case class UpdateLineItemsPayload(sku: String, quantity: Int, attributes: Option[Json] = None)
}
