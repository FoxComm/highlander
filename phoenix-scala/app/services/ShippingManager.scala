package services

import failures.NotFoundFailure404
import failures.ShippingMethodFailures.{ShippingMethodNotApplicableToCart, ShippingMethodNotFound}
import models.account._
import models.cord._
import models.cord.lineitems._
import models.inventory.Sku
import models.location.Region
import models.objects._
import models.rules.{Condition, QueryStatement}
import models.shipping.{ShippingMethod, ShippingMethods}
import services.carts.getCartByOriginator
import utils.JsonFormatters
import utils.aliases._
import utils.db._
import slick.driver.PostgresDriver.api._
import org.json4s.JsonAST._
import payloads.ShippingMethodPayloadsPayloads.{CreateShippingMethodPayload, UpdateShippingMethodPayload}

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  case class ShippingData(cart: Cart,
                          cartTotal: Int,
                          cartSubTotal: Int,
                          shippingAddress: Option[OrderShippingAddress] = None,
                          shippingRegion: Option[Region] = None,
                          lineItems: Seq[CartLineItemProductData])

  def getShippingMethods()(implicit ec: EC,
                           db: DB): DbResultT[Seq[responses.AdminShippingMethodsResponse.Root]] =
    for {
      shipMethods ← * <~ ShippingMethods.findActive.result
    } yield shipMethods.map(responses.AdminShippingMethodsResponse.build)

  def getShippingMethodById(
      id: Int)(implicit ec: EC, db: DB): DbResultT[responses.AdminShippingMethodsResponse.Root] =
    for {
      shipMethod ← * <~ ShippingMethods
                    .findActiveById(id)
                    .mustFindOneOr(ShippingMethodNotFound(id))
    } yield responses.AdminShippingMethodsResponse.build(shipMethod)

  def createShippingMethod(payload: CreateShippingMethodPayload)(
      implicit ec: EC,
      db: DB): DbResultT[responses.AdminShippingMethodsResponse.Root] =
    for {
      shipMethod ← * <~ ShippingMethod.buildFromCreatePayload(payload)
      created    ← * <~ ShippingMethods.create(shipMethod)
    } yield responses.AdminShippingMethodsResponse.build(created)

  def updateShippingMethod(id: Int, payload: UpdateShippingMethodPayload)(
      implicit ec: EC,
      db: DB): DbResultT[responses.AdminShippingMethodsResponse.Root] =
    for {
      oldShipMethod ← * <~ ShippingMethods
                       .findActiveById(id)
                       .mustFindOneOr(ShippingMethodNotFound(id))
      newShipMethod ← * <~ ShippingMethod.buildFromUpdatePayload(oldShipMethod, payload)
      _             ← * <~ ShippingMethods.update(oldShipMethod, oldShipMethod.copy(isActive = false))
      created       ← * <~ ShippingMethods.create(newShipMethod)
    } yield responses.AdminShippingMethodsResponse.build(created)

  def softDeleteShippingMethod(id: Int)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      shipMethod ← * <~ ShippingMethods
                    .findActiveById(id)
                    .mustFindOneOr(ShippingMethodNotFound(id))
      _ ← * <~ ShippingMethods.update(shipMethod, shipMethod.copy(isActive = false))
    } yield {}

  def getShippingMethodsForCart(originator: User)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[responses.ShippingMethodsResponse.Root]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, None)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
      response = shipMethods.collect {
        case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
          val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
          responses.ShippingMethodsResponse.build(sm, !restricted)
      }
    } yield response

  def getShippingMethodsForCart(refNum: String, customer: Option[User] = None)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[responses.ShippingMethodsResponse.Root]] =
    for {
      cart        ← * <~ findByRefNumAndOptionalCustomer(refNum, customer)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
      response = shipMethods.collect {
        case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
          val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
          responses.ShippingMethodsResponse.build(sm, !restricted)
      }
    } yield response

  private def findByRefNumAndOptionalCustomer(refNum: String, customer: Option[User] = None)(
      implicit ec: EC,
      db: DB): DbResultT[Cart] = customer match {
    case Some(c) ⇒
      Carts
        .findByRefNumAndAccountId(refNum, c.accountId)
        .mustFindOneOr(NotFoundFailure404(Carts, refNum))
    case _ ⇒ Carts.mustFindByRefNum(refNum)
  }

  def evaluateShippingMethodForCart(shippingMethod: ShippingMethod,
                                    cart: Cart)(implicit ec: EC, db: DB): DbResultT[Unit] = {
    getShippingData(cart).flatMap { shippingData ⇒
      val failure = ShippingMethodNotApplicableToCart(shippingMethod.id, cart.refNum)
      if (QueryStatement.evaluate(shippingMethod.conditions, shippingData, evaluateCondition)) {
        val hasRestrictions =
          QueryStatement.evaluate(shippingMethod.restrictions, shippingData, evaluateCondition)
        if (hasRestrictions) DbResultT.failure(failure) else DbResultT.unit
      } else {
        DbResultT.failure(failure)
      }
    }
  }

  private def getShippingData(cart: Cart)(implicit ec: EC, db: DB): DbResultT[ShippingData] =
    for {
      orderShippingAddress ← * <~ OrderShippingAddresses
                              .findByOrderRefWithRegions(cart.refNum)
                              .result
                              .headOption

      lineItems ← * <~ LineItemManager.getCartLineItems(cart.refNum)
    } yield
      ShippingData(cart = cart,
                   cartTotal = cart.grandTotal,
                   cartSubTotal = cart.subTotal,
                   shippingAddress = orderShippingAddress.map(_._1),
                   shippingRegion = orderShippingAddress.map(_._2),
                   lineItems = lineItems)

  private def evaluateCondition(cond: Condition, shippingData: ShippingData): Boolean = {
    cond.rootObject match {
      case "Order"           ⇒ evaluateOrderCondition(shippingData, cond)
      case "ShippingAddress" ⇒ evaluateShippingAddressCondition(shippingData, cond)
      case "LineItems"       ⇒ evaluateLineItemsCondition(shippingData, cond)
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

  private val COUNT_TAG         = "countTag-"
  private val COUNT_WITHOUT_TAG = "countWithoutTag-"

  private def evaluateLineItemsCondition(shippingData: ShippingData,
                                         condition: Condition): Boolean =
    condition.field match {
      case "count" ⇒ Condition.matches(shippingData.lineItems.size, condition)
      case t if t startsWith COUNT_TAG ⇒ {
        val tag = t.substring(COUNT_TAG.length)
        Condition.matches(lineItemsWithTag(shippingData, tag), condition)
      }
      case t if t startsWith COUNT_WITHOUT_TAG ⇒ {
        val tag = t.substring(COUNT_WITHOUT_TAG.length)
        Condition
          .matches(shippingData.lineItems.size - lineItemsWithTag(shippingData, tag), condition)
      }
      case _ ⇒ false
    }

  private def lineItemsWithTag(shippingData: ShippingData, tag: String): Int =
    shippingData.lineItems.count { l ⇒
      hasTag(l, tag)
    }

  private def hasTag(lineItem: CartLineItemProductData, tag: String): Boolean =
    ObjectUtils.get("tags", lineItem.productForm, lineItem.productShadow) match {
      case JArray(tags) ⇒
        tags.foldLeft(false) { (res, jtag) ⇒
          jtag match {
            case JString(t) ⇒ res || t.contains(tag)
            case _          ⇒ res
          }
        }
      case _ ⇒ false
    }

}
