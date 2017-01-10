package models.discount.qualifiers

import models.discount.DiscountInput
import models.product.Mvp
import services._
import utils.aliases._

case class OrderTotalAmountQualifier(totalAmount: Int) extends Qualifier {

  val qualifierType: QualifierType = OrderTotalAmount

  def check(input: DiscountInput)(implicit db: DB, ec: EC, es: ES, au: AU): Result[Unit] = {
    val purchasedGcTotal = input.lineItems.map { lineItem ⇒
      for {
        attrs ← lineItem.attributes
        _     ← attrs.giftCard
      } yield Mvp.priceAsInt(lineItem.productVariantForm, lineItem.productVariantShadow)
    }.flatten.sum

    if (input.cart.subTotal - purchasedGcTotal >= totalAmount) accept()
    else reject(input, s"Order subtotal is less than $totalAmount")
  }
}
