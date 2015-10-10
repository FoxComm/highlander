package services

import models.{OrderLineItemSkus, Sku, Skus, OrderLineItems, Order, OrderShippingAddress, Region, ShippingMethods}
import models.OrderLineItems.scope._
import models.rules.{Condition, QueryStatement}
import scala.concurrent.ExecutionContext
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import Result._
import utils.Slick.DbResult

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  final case class ShippingData(order: Order, orderTotal: Int, orderSubTotal: Int,
    shippingAddress: Option[OrderShippingAddress] = None, shippingRegion: Option[Region] = None, skus: Seq[Sku])

  def getShippingMethodsForOrder(order: models.Order)
    (implicit db: Database, ec: ExecutionContext): DbResult[Seq[responses.ShippingMethods.Root]] = {
    ShippingMethods.findActive.result.flatMap { shippingMethods ⇒
      getShippingData(order).flatMap { shippingData ⇒
        val methodResponses = shippingMethods.collect {
          case sm if QueryStatement.evaluate(sm.conditions, shippingData, evaluateCondition) ⇒
            val restricted = QueryStatement.evaluate(sm.restrictions, shippingData, evaluateCondition)
            responses.ShippingMethods.build(sm, !restricted)
        }

        DbResult.good(methodResponses)
      }
    }
  }

  def evaluateShippingMethodForOrder(shippingMethod: models.ShippingMethod, order: Order)
    (implicit db: Database, ec: ExecutionContext): DbResult[Boolean] = {
    getShippingData(order).flatMap {
      case shippingData if QueryStatement.evaluate(shippingMethod.conditions, shippingData, evaluateCondition) ⇒
        DbResult.good(QueryStatement.evaluate(shippingMethod.restrictions, shippingData, evaluateCondition))
      case _ ⇒
        DbResult.failure(GeneralFailure("Unable to retrieve shipping data"))
    }
  }

  private def getShippingData(order: Order)(implicit db: Database, ec: ExecutionContext): DBIO[ShippingData] = {
    for {
      orderShippingAddress ← models.OrderShippingAddresses.findByOrderIdWithRegions(order.id).result.headOption
      subTotal ← OrderTotaler._subTotalForOrder(order)
      grandTotal ← OrderTotaler._grandTotalForOrder(order)
      skus ← (for {
        lineItems ← OrderLineItems._findByOrder(order)
        skus ← Skus if skus.id === lineItems.skuId
      } yield skus).result
    } yield ShippingData(
      order = order,
      orderTotal = grandTotal.getOrElse(0),
      orderSubTotal = subTotal.getOrElse(0),
      shippingAddress = orderShippingAddress.map(_._1),
      shippingRegion = orderShippingAddress.map(_._2),
      skus = skus)
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
    shippingData.shippingAddress.fold(false) { shippingAddress ⇒
      condition.field match {
        case "address1" ⇒
          Condition.matches(shippingAddress.address1, condition)
        case "address2" ⇒
          Condition.matches(shippingAddress.address2, condition)
        case "city" ⇒
          Condition.matches(shippingAddress.city, condition)
        case "regionId" ⇒
          Condition.matches(shippingAddress.regionId, condition)
        case "regionName" ⇒
          shippingData.shippingRegion.fold(false)(sr ⇒ Condition.matches(sr.name, condition))
        case "regionAbbrev" ⇒
          shippingData.shippingRegion.fold(false)(sr ⇒ Condition.matches(sr.abbreviation, condition))
        case "zip" ⇒
          Condition.matches(shippingAddress.zip, condition)
        case _ ⇒
          false
      }
    }
  }
}
