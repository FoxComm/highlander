package concepts.discounts

import cats.implicits._
import concepts.discounts.qualifiers._
import concepts.discounts.offers._
import models.order.lineitems.{OrderLineItemProductData, OrderLineItemSkus}
import models.order.{Order, OrderShippingMethods}
import models.shipping
import models.shipping.ShippingMethod
import services.Result
import slick.driver.PostgresDriver.api._
import utils.Slick.DbResult
import utils.Slick.implicits._
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.aliases._

object DiscountExecutor {

  def getAdjustments(order: Order, qualifier: Qualifier, offer: Offer)
    (implicit ec: EC, db: DB): Result[Seq[LineItemAdjustment]] = (for {

    orderDetails ← * <~ fetchOrderDetails(order).toXor
    (li, sm)     = orderDetails
    _            ← * <~ qualifier.check(order, li, sm)
    adj          ← * <~ offer.adjust(order, li, sm)
  } yield adj).run()


  private def fetchOrderDetails(order: Order)(implicit ec: EC, db: DB) = {
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