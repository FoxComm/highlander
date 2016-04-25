package payloads

import cats.data._
import cats.implicits._
import failures.Failure
import utils.Money._
import utils.Validation
import Validation._

case class UpdateLineItemsPayload(sku: String, quantity: Int)

case class AddGiftCardLineItem(balance: Int, currency: Currency = Currency.USD)
  extends Validation[AddGiftCardLineItem] {

  def validate: ValidatedNel[Failure, AddGiftCardLineItem] = {
    greaterThan(balance, 0, "Balance").map { case _ ⇒ this }
  }
}