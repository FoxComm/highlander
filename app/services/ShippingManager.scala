package services

import failures.NotFoundFailure404
import failures.ShippingMethodFailures.ShippingMethodNotApplicableToOrder
import models.cord._
import models.cord.lineitems.OrderLineItemSkus
import models.customer.Customer
import models.inventory.{Sku, Skus}
import models.location.Region
import models.rules.{Condition, QueryStatement}
import models.shipping.{ShippingMethod, ShippingMethods}
import models.traits.Originator
import services.carts.getCartByOriginator
import slick.driver.PostgresDriver.api._
import utils.JsonFormatters
import utils.aliases._
import utils.db._

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  case class ShippingData(cart: Cart,
                          cartTotal: Int,
                          cartSubTotal: Int,
                          shippingAddress: Option[OrderShippingAddress] = None,
                          shippingRegion: Option[Region] = None,
                          skus: Seq[Sku])

  def getShippingMethodsForCart(originator: Originator)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[responses.ShippingMethods.Root]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, None)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
      response = shipMethods.collect {
        case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
          val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
          responses.ShippingMethods.build(sm, !restricted)
      }
    } yield response

  def getShippingMethodsForCart(refNum: String, customer: Option[Customer] = None)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[responses.ShippingMethods.Root]] =
    for {
      cart        ← * <~ findByRefNumAndOptionalCustomer(refNum, customer)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
      response = shipMethods.collect {
        case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
          val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
          responses.ShippingMethods.build(sm, !restricted)
      }
    } yield response

  private def findByRefNumAndOptionalCustomer(refNum: String, customer: Option[Customer] = None)(
      implicit ec: EC,
      db: DB): DbResultT[Cart] = customer match {
    case Some(c) ⇒
      Carts.findByRefNumAndCustomer(refNum, c).mustFindOneOr(NotFoundFailure404(Carts, refNum))
    case _ ⇒ Carts.mustFindByRefNum(refNum)
  }

  def evaluateShippingMethodForCart(shippingMethod: ShippingMethod, cart: Cart)(
      implicit ec: EC): DbResultT[Unit] = {
    getShippingData(cart).toXor.flatMap { shippingData ⇒
      val failure = ShippingMethodNotApplicableToOrder(shippingMethod.id, cart.refNum)
      if (QueryStatement.evaluate(shippingMethod.conditions, shippingData, evaluateCondition)) {
        val hasRestrictions =
          QueryStatement.evaluate(shippingMethod.restrictions, shippingData, evaluateCondition)
        if (hasRestrictions) DbResultT.failure(failure) else DbResultT.unit
      } else {
        DbResultT.failure(failure)
      }
    }
  }

  private def getShippingData(cart: Cart)(implicit ec: EC): DBIO[ShippingData] =
    for {
      orderShippingAddress ← OrderShippingAddresses
                              .findByOrderRefWithRegions(cart.refNum)
                              .result
                              .headOption
      skus ← (for {
              liSku ← OrderLineItemSkus.findByOrderRef(cart.refNum)
              skus  ← Skus if skus.id === liSku.skuId
            } yield skus).result
    } yield
      ShippingData(cart = cart,
                   cartTotal = cart.grandTotal,
                   cartSubTotal = cart.subTotal,
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
      case "subtotal"   ⇒ Condition.matches(shippingData.cartSubTotal, condition)
      case "grandtotal" ⇒ Condition.matches(shippingData.cartTotal, condition)
      case _            ⇒ false
    }
  }

  private def evaluateShippingAddressCondition(shippingData: ShippingData,
                                               condition: Condition): Boolean = {
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
