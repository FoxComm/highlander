package payloads

import utils.aliases._

object DiscountPayloads {

  case class CreateDiscountForm(attributes: Json)

  case class CreateDiscountShadow(attributes: Json)

  case class CreateDiscount(form: CreateDiscountForm, shadow: CreateDiscountShadow)

  case class UpdateDiscountForm(attributes: Json)

  case class UpdateDiscountShadow(attributes: Json)

  case class UpdateDiscount(form: UpdateDiscountForm, shadow: UpdateDiscountShadow)
}
