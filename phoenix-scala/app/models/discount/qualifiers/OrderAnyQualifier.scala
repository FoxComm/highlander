package models.discount.qualifiers

import io.circe.Json
import models.discount.DiscountInput
import utils.aliases._
import utils.apis.Apis
import utils.db._

case object OrderAnyQualifier extends Qualifier {

  val qualifierType: QualifierType = OrderAny

  def check(input: DiscountInput)(implicit db: DB, ec: EC, apis: Apis, au: AU): Result[Unit] =
    if (input.isEligibleForDiscount) accept()
    else reject(input, "Items in cart are not eligible for discount")

  // yep, it's backward compatible with json4s behaviour
  def json: Json = Json.obj()
}
