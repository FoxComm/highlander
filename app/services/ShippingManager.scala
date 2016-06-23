package services

import failures.NotFoundFailure404
import failures.ShippingMethodFailures.ShippingMethodNotApplicableToOrder
import models.customer.Customer
import models.inventory.{Sku, Skus}
import models.location.Region
import models.order._
import models.order.lineitems.OrderLineItemSkus
import models.rules.{Condition, QueryStatement}
import models.shipping.{ShippingMethod, ShippingMethods}
import models.traits.Originator
import services.orders.getCartByOriginator
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  case class ShippingData(order: Order,
                          orderTotal: Int,
                          orderSubTotal: Int,
                          shippingAddress: Option[OrderShippingAddress] = None,
                          shippingRegion: Option[Region] = None,
                          skus: Seq[Sku])

  def getShippingMethodsForCart(originator: Originator)(
      implicit ec: EC, db: DB): DbResultT[Seq[responses.ShippingMethods.Root]] =
    for {
      order       ← * <~ getCartByOriginator(originator, None)
      _           ← * <~ order.mustBeCart
      shipMethods ← * <~ ShippingMethods.findActive.result.toXor
      shipData    ← * <~ getShippingData(order).toXor
      response = shipMethods.collect {
        case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
          val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
          responses.ShippingMethods.build(sm, !restricted)
      }
    } yield response

  def getShippingMethodsForOrder(refNum: String, customer: Option[Customer] = None)(
      implicit ec: EC, db: DB): DbResultT[Seq[responses.ShippingMethods.Root]] =
    for {
      order       ← * <~ findByRefNumAndOptionalCustomer(refNum, customer)
      shipMethods ← * <~ ShippingMethods.findActive.result.toXor
      shipData    ← * <~ getShippingData(order).toXor
      response = shipMethods.collect {
        case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
          val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
          responses.ShippingMethods.build(sm, !restricted)
      }
    } yield response

  private def findByRefNumAndOptionalCustomer(refNum: String, customer: Option[Customer] = None)(
      implicit ec: EC, db: DB): DbResult[Order] = customer match {
    case Some(c) ⇒
      Orders
        .findOneByRefNumAndCustomer(refNum, c)
        .mustFindOneOr(NotFoundFailure404(Orders, refNum))
    case _ ⇒ Orders.mustFindByRefNum(refNum)
  }

  def evaluateShippingMethodForOrder(shippingMethod: ShippingMethod, order: Order)(
      implicit ec: EC): DbResult[Unit] = {
    getShippingData(order).flatMap { shippingData ⇒
      val failure = ShippingMethodNotApplicableToOrder(shippingMethod.id, order.refNum)
      if (QueryStatement.evaluate(shippingMethod.conditions, shippingData, evaluateCondition)) {
        val hasRestrictions =
          QueryStatement.evaluate(shippingMethod.restrictions, shippingData, evaluateCondition)
        if (hasRestrictions) DbResult.failure(failure) else DbResult.unit
      } else {
        DbResult.failure(failure)
      }
    }
  }

  private def getShippingData(order: Order)(implicit ec: EC): DBIO[ShippingData] =
    for {
      orderShippingAddress ← OrderShippingAddresses
                              .findByOrderIdWithRegions(order.id)
                              .result
                              .headOption
      skus ← (for {
              liSku ← OrderLineItemSkus.findByOrderId(order.id)
              skus  ← Skus if skus.id === liSku.skuId
            } yield skus).result
    } yield
      ShippingData(order = order,
                   orderTotal = order.grandTotal,
                   orderSubTotal = order.subTotal,
                   shippingAddress = orderShippingAddress.map(_._1),
                   shippingRegion = orderShippingAddress.map(_._2),
                   skus = skus)

  private def evaluateCondition(cond: Condition, shippingData: ShippingData): Boolean = {
    cond.rootObject match {
      case "Order"           ⇒ evaluateOrderCondition(shippingData, cond)
      case "ShippingAddress" ⇒ evaluateShippingAddressCondition(shippingData, cond)
      case _                 ⇒ false
    }
  }

  private def evaluateOrderCondition(shippingData: ShippingData, condition: Condition): Boolean = {
    condition.field match {
      case "subtotal"   ⇒ Condition.matches(shippingData.orderSubTotal, condition)
      case "grandtotal" ⇒ Condition.matches(shippingData.orderTotal, condition)
      case _            ⇒ false
    }
  }

  private def evaluateShippingAddressCondition(
      shippingData: ShippingData, condition: Condition): Boolean = {
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
        case "countryId" ⇒
          shippingData.shippingRegion.fold(false)(sr ⇒ Condition.matches(sr.countryId, condition))
        case "regionName" ⇒
          shippingData.shippingRegion.fold(false)(sr ⇒ Condition.matches(sr.name, condition))
        case "regionAbbrev" ⇒
          shippingData.shippingRegion.fold(false)(sr ⇒
                Condition.matches(sr.abbreviation, condition))
        case "zip" ⇒
          Condition.matches(shippingAddress.zip, condition)
        case _ ⇒
          false
      }
    }
  }
}
