package services

import com.github.tminglei.slickpg.LTree
import failures.{GeneralFailure, NotFoundFailure404}
import failures.ShippingMethodFailures.ShippingMethodNotApplicableToCart
import models.account._
import models.cord._
import models.cord.lineitems._
import models.inventory.Sku
import models.location.{Countries, Region}
import models.objects._
import models.rules.{Condition, QueryStatement}
import models.shipping.{DefaultShippingMethod, DefaultShippingMethods, ShippingMethod, ShippingMethods}
import services.carts.getCartByOriginator
import utils.JsonFormatters
import utils.aliases._
import utils.db._
import slick.driver.PostgresDriver.api._
import org.json4s.JsonAST._
import responses.ShippingMethodsResponse
import cats.implicits._
import failures.AddressFailures.NoCountryFound
import payloads.ShippingMethodsPayloads.RegionSearchPayload

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats
  def countryCode = """([a-zA-Z]{2,3})""".r

  case class ShippingData(cart: Cart,
                          cartTotal: Int = 0,
                          cartSubTotal: Int = 0,
                          shippingAddress: Option[OrderShippingAddress] = None,
                          shippingRegion: Option[Region] = None,
                          lineItems: Seq[CartLineItemProductData] = Seq())

  def emptyShippingData = ShippingData(cart = Cart(scope = LTree(""), accountId = 0))

  def setDefault(shippingMethodId: Int)(implicit ec: EC,
                                        db: DB,
                                        au: AU): DbResultT[ShippingMethodsResponse.Root] =
    for {
      shippingMethod ← * <~ ShippingMethods.mustFindById404(shippingMethodId)
      _ ← * <~ DefaultShippingMethods.create(
             DefaultShippingMethod(scope = Scope.current, shippingMethodId = shippingMethodId))
    } yield ShippingMethodsResponse.build(shippingMethod)

  def removeDefault()(implicit ec: EC, au: AU): DbResultT[Option[ShippingMethodsResponse.Root]] = {
    val scope = Scope.current
    for {
      shippingMethod ← * <~ DefaultShippingMethods.resolve(scope)
      _              ← * <~ DefaultShippingMethods.findDefaultByScope(scope).deleteAll
    } yield shippingMethod.map(ShippingMethodsResponse.build(_))
  }

  def getDefault(implicit ec: EC, au: AU): DbResultT[Option[ShippingMethodsResponse.Root]] =
    for {
      shippingMethod ← * <~ DefaultShippingMethods.resolve(Scope.current)
    } yield shippingMethod.map(ShippingMethodsResponse.build(_))

  def getActive(implicit ec: EC): DbResultT[Seq[ShippingMethodsResponse.Root]] =
    for {
      shippingMethods ← * <~ ShippingMethods.findActive.result
    } yield shippingMethods.map(responses.ShippingMethodsResponse.build(_))

  def getShippingMethodsForCart(originator: User)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[responses.ShippingMethodsResponse.Root]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, None)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
      response = filter(shipMethods, shipData)
    } yield response

  def getShippingMethodsForRegion(countryCode: String)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[responses.ShippingMethodsResponse.Root]] =
    for {
      shipMethods ← * <~ ShippingMethods.findActive.result
      country     ← * <~ Countries.findByCode(countryCode).mustFindOneOr(NoCountryFound(countryCode))

      shipToRegion = emptyShippingData.copy(
          shippingRegion = Region(countryId = country.id, name = country.name).some)

      _ ← * <~ println(s"evaluate $shipToRegion with $shipMethods")

      response = filter(shipMethods, shipToRegion)

      _ ← * <~ println(s"got $response")

    } yield response

  def getShippingMethodsForCart(refNum: String, customer: Option[User] = None)(
      implicit ec: EC,
      db: DB): DbResultT[Seq[responses.ShippingMethodsResponse.Root]] =
    for {
      cart        ← * <~ findByRefNumAndOptionalCustomer(refNum, customer)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
      response = filter(shipMethods, shipData)
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

  def filter(shipMethods: Seq[ShippingMethod], shipData: ShippingData) = shipMethods.collect {
    case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
      val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
      val a          = responses.ShippingMethodsResponse.build(sm, !restricted)
      println(s"collected $a with restricted status $restricted")
      a
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
    println(s"$shippingData with $condition")

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
        case "countryId" ⇒ {
          val fold = shippingData.shippingRegion.fold(false)(sr ⇒
                Condition.matches(sr.countryId, condition))
          println(s"!!! $shippingData with $condition has result $fold")
          fold
        }
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
