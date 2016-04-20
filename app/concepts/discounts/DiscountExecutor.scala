package concepts.discounts

import concepts.discounts.qualifiers._
import concepts.discounts.offers._
import models.order.lineitems.{OrderLineItemProductData, OrderLineItemSkus}
import models.order.Order
import models.shipping
import services.Result
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object DiscountExecutor {

  def getAdjustments(order: Order, qualifier: Qualifier, offer: Offer)
    (implicit ec: EC, db: DB): Result[Seq[LineItemAdjustment]] = (for {

    orderDetails ← * <~ fetchOrderDetails(order).toXor
    (li, sm)     = orderDetails
    _            ← * <~ qualifier.check(order, li, sm)
    adj          ← * <~ offer.adjust(order, li, sm)
  } yield adj).run()


  private def fetchOrderDetails(order: Order)(implicit ec: EC) = {
    for {
      lineItemTup ← OrderLineItemSkus.findLineItemsByOrder(order).result
      lineItems   = lineItemTup.map {
        case (sku, skuForm, skuShadow, lineItem) ⇒
          OrderLineItemProductData(sku, skuForm, skuShadow, lineItem)
      }
      shipMethod  ← shipping.ShippingMethods.forOrder(order).one
    } yield (
      lineItems,
      shipMethod
    )
  }
}