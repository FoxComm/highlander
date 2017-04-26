package models.discount.qualifiers

import io.circe.syntax._
import models.discount._
import utils.aliases._
import utils.apis.Apis
import utils.db._
import utils.json.codecs._

case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderTotalAmount

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    if (input.eligibleForDiscountSubtotal >= totalAmount) accept()
    else reject(input, s"Order subtotal is less than $totalAmount")

  def json: Json = this.asJson
}
