package services

import models.{Sku, OrderShippingAddresses, OrderLineItem, Skus, OrderLineItems, Order, OrderShippingAddress, Region,
ShippingMethods}
import models.rules.{Condition, QueryStatement}
import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import Result._
import utils.Slick.DbResult

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  final case class ShippingData(order: Order, orderTotal: Int, orderSubTotal: Int,
    shippingAddress: OrderShippingAddress, shippingRegion: Region, skus: Seq[Sku])

  def getShippingMethodsForOrder(order: models.Order)
    (implicit db: Database, ec: ExecutionContext): DbResult[Seq[responses.ShippingMethods.Root]] = {

    val queries = for {
      orderShippingAddresses ← models.OrderShippingAddresses.findByOrderIdWithRegions(order.id).result.headOption
      subTotal ← OrderTotaler._subTotalForOrder(order)
      grandTotal ← OrderTotaler._grandTotalForOrder(order)
      shippingMethods ← ShippingMethods.findActive.result
      skus ← (for {
        lineItems ← OrderLineItems._findByOrder(order)
        skus ← Skus if skus.id === lineItems.skuId
      } yield skus).result
    } yield (orderShippingAddresses, subTotal, grandTotal, shippingMethods, skus)

    queries.flatMap {
      case (Some((address, region)), subTotal, grandTotal, shippingMethods, skus) ⇒

        val shippingData = ShippingData(order, grandTotal.getOrElse(0), subTotal.getOrElse(0), address, region, skus)

        val methodResponses = shippingMethods.collect {
          case sm if QueryStatement.evaluate(sm.conditions, shippingData, evaluateCondition) ⇒
            val restricted = QueryStatement.evaluate(sm.restrictions, shippingData, evaluateCondition)
            responses.ShippingMethods.build(sm, !restricted)
        }

        DbResult.good(methodResponses)

      case (None, _, _, _, _) ⇒
        DbResult.failure(OrderShippingMethodsCannotBeProcessed(order.refNum))
    }
  }

  private def evaluateCondition(cond: Condition, shippingData: ShippingData): Boolean = {
    cond.rootObject match {
      case "Order" ⇒ evaluateOrderCondition(shippingData, cond)
      case "ShippingAddress" ⇒ evaluateShippingAddressCondition(shippingData, cond)
      case _ ⇒ false
    }
  }

  private def evaluateOrderCondition(shippingData: ShippingData, condition: Condition): Boolean = {
    condition.field match {
      case "subtotal" ⇒ Condition.matches(shippingData.orderSubTotal, condition)
      case "grandtotal" ⇒ Condition.matches(shippingData.orderTotal, condition)
      case "skus.isHazardous" ⇒ shippingData.skus.exists(sku ⇒ Condition.matches(sku.isHazardous, condition))
      case _ ⇒ false
    }
  }

  private def evaluateShippingAddressCondition(shippingData: ShippingData, condition: Condition): Boolean = {
    condition.field match {
      case "address1" ⇒
        Condition.matches(shippingData.shippingAddress.address1, condition)
      case "address2" ⇒
        Condition.matches(shippingData.shippingAddress.address2, condition)
      case "city" ⇒
        Condition.matches(shippingData.shippingAddress.city, condition)
      case "regionId" ⇒
        Condition.matches(shippingData.shippingAddress.regionId, condition)
      case "regionName" ⇒
        Condition.matches(shippingData.shippingRegion.name, condition)
      case "regionAbbrev" ⇒
        Condition.matches(shippingData.shippingRegion.abbreviation, condition)
      case "zip" ⇒
        Condition.matches(shippingData.shippingAddress.zip, condition)
      case _ ⇒
        false
    }
  }
}
