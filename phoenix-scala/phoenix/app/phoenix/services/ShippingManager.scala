package phoenix.services

import cats.implicits._
import com.github.tminglei.slickpg.LTree
import core.db._
import core.failures.NotFoundFailure404
import objectframework.ObjectUtils
import org.json4s.JsonAST._
import phoenix.failures.ShippingMethodFailures.ShippingMethodNotApplicableToCart
import phoenix.models.account._
import phoenix.models.cord._
import phoenix.models.cord.lineitems._
import phoenix.models.location.{Countries, Region}
import phoenix.models.rules.{Condition, QueryStatement}
import phoenix.models.shipping._
import phoenix.services.carts.getCartByOriginator
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._
import phoenix.responses.ShippingMethodsResponse
import phoenix.responses.ShippingMethodsResponse.Root
import phoenix.failures.AddressFailures.NoCountryFound

object ShippingManager {
  implicit val formats = JsonFormatters.phoenixFormats

  case class ShippingData(cart: Cart,
                          cartTotal: Long,
                          cartSubTotal: Long,
                          shippingAddress: Option[OrderShippingAddress] = None,
                          shippingRegion: Option[Region] = None,
                          lineItems: Seq[CartLineItemProductData] = Seq())

  def setDefault(shippingMethodId: Int)(implicit ec: EC, db: DB, au: AU): DbResultT[Root] =
    for {
      shippingMethod ← * <~ ShippingMethods.mustFindById404(shippingMethodId)
      _ ← * <~ DefaultShippingMethods.create(
           DefaultShippingMethod(scope = Scope.current, shippingMethodId = shippingMethodId))
    } yield ShippingMethodsResponse.build(shippingMethod)

  def removeDefault()(implicit ec: EC, au: AU): DbResultT[Option[Root]] = {
    val scope = Scope.current
    for {
      shippingMethod ← * <~ DefaultShippingMethods.resolve(scope)
      _              ← * <~ DefaultShippingMethods.findDefaultByScope(scope).deleteAll
    } yield shippingMethod.map(ShippingMethodsResponse.build(_))
  }

  def getDefault(implicit ec: EC, au: AU): DbResultT[Option[Root]] =
    for {
      shippingMethod ← * <~ DefaultShippingMethods.resolve(Scope.current)
    } yield shippingMethod.map(ShippingMethodsResponse.build(_))

  def getActive(implicit ec: EC): DbResultT[Seq[Root]] =
    for {
      shippingMethods ← * <~ ShippingMethods.findActive.result
    } yield shippingMethods.map(ShippingMethodsResponse.build(_))

  def getShippingMethodsForCart(originator: User)(implicit ec: EC, db: DB): DbResultT[Seq[Root]] =
    for {
      cart        ← * <~ getCartByOriginator(originator, None)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
    } yield filterMethods(shipMethods, shipData)

  def getShippingMethodsForRegion(countryCode: String, originator: User)(implicit ec: EC,
                                                                         db: DB): DbResultT[Seq[Root]] =
    for {
      cart     ← * <~ getCartByOriginator(originator, None)
      shipData ← * <~ getShippingData(cart)
      country  ← * <~ Countries.findByCode(countryCode).mustFindOneOr(NoCountryFound(countryCode))

      shipMethods ← * <~ ShippingMethods.findActive.result
      shipToRegion = shipData.copy(shippingRegion = Region(countryId = country.id, name = country.name).some)
    } yield filterMethods(shipMethods, shipToRegion)

  def getShippingMethodsForCart(refNum: String, customer: Option[User] = None)(implicit ec: EC,
                                                                               db: DB): DbResultT[Seq[Root]] =
    for {
      cart        ← * <~ findByRefNumAndOptionalCustomer(refNum, customer)
      shipMethods ← * <~ ShippingMethods.findActive.result
      shipData    ← * <~ getShippingData(cart)
    } yield filterMethods(shipMethods, shipData)

  private def findByRefNumAndOptionalCustomer(refNum: String, customer: Option[User] = None)(
      implicit ec: EC,
      db: DB): DbResultT[Cart] = customer match {
    case Some(c) ⇒
      Carts
        .findByRefNumAndAccountId(refNum, c.accountId)
        .mustFindOneOr(NotFoundFailure404(Carts, refNum))
    case _ ⇒ Carts.mustFindByRefNum(refNum)
  }

  def evaluateShippingMethodForCart(shippingMethod: ShippingMethod, cart: Cart)(implicit ec: EC,
                                                                                db: DB): DbResultT[Unit] =
    getShippingData(cart).flatMap { shippingData ⇒
      val failure = ShippingMethodNotApplicableToCart(shippingMethod.id, cart.refNum)
      if (QueryStatement.evaluate(shippingMethod.conditions, shippingData, evaluateCondition)) {
        val hasRestrictions =
          QueryStatement.evaluate(shippingMethod.restrictions, shippingData, evaluateCondition)
        if (hasRestrictions) DbResultT.failure(failure) else ().pure[DbResultT]
      } else {
        DbResultT.failure(failure)
      }
    }

  def filterMethods(shipMethods: Seq[ShippingMethod], shipData: ShippingData): Seq[Root] =
    shipMethods.collect {
      case sm if QueryStatement.evaluate(sm.conditions, shipData, evaluateCondition) ⇒
        val restricted = QueryStatement.evaluate(sm.restrictions, shipData, evaluateCondition)
        ShippingMethodsResponse.build(sm, isEnabled = !restricted)
    }

  private def getShippingData(cart: Cart)(implicit ec: EC, db: DB): DbResultT[ShippingData] =
    for {
      orderShippingAddress ← * <~ OrderShippingAddresses
                              .findByOrderRefWithRegions(cart.refNum)
                              .result
                              .headOption

      lineItems ← * <~ LineItemManager.getCartLineItems(cart.refNum)
    } yield
      ShippingData(
        cart = cart,
        cartTotal = cart.grandTotal,
        cartSubTotal = cart.subTotal,
        shippingAddress = orderShippingAddress.map(_._1),
        shippingRegion = orderShippingAddress.map(_._2),
        lineItems = lineItems
      )

  private def evaluateCondition(cond: Condition, shippingData: ShippingData): Boolean =
    cond.rootObject match {
      case "Order"           ⇒ evaluateOrderCondition(shippingData, cond)
      case "ShippingAddress" ⇒ evaluateShippingAddressCondition(shippingData, cond)
      case "LineItems"       ⇒ evaluateLineItemsCondition(shippingData, cond)
      case _                 ⇒ false
    }

  private def evaluateOrderCondition(shippingData: ShippingData, condition: Condition): Boolean =
    condition.field match {
      case "subtotal" ⇒
        Condition.matches(shippingData.cartSubTotal.toInt, condition) // FIXME @aafa .toInt
      case "grandtotal" ⇒
        Condition.matches(shippingData.cartTotal.toInt, condition) // FIXME @aafa .toInt
      case _ ⇒ false
    }

  private def evaluateShippingAddressCondition(shippingData: ShippingData, condition: Condition): Boolean =
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
        case "zip" ⇒
          Condition.matches(shippingAddress.zip, condition)
        case _ ⇒
          false
      }
    // @aafa Here I extracted shippingRegion matching against incoming queries
    // since we can have address and region provided separately within shippingData
    } || shippingData.shippingRegion.fold(false)(shippingRegion ⇒
      condition.field match {
        case "countryId" ⇒
          Condition.matches(shippingRegion.countryId, condition)
        case "regionName" ⇒
          Condition.matches(shippingRegion.name, condition)
        case "regionAbbrev" ⇒
          Condition.matches(shippingRegion.abbreviation, condition)
        case _ ⇒ false
    })

  private val COUNT_TAG         = "countTag-"
  private val COUNT_WITHOUT_TAG = "countWithoutTag-"

  private def evaluateLineItemsCondition(shippingData: ShippingData, condition: Condition): Boolean =
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
        tags.exists {
          case JString(t) ⇒ t.contains(tag)
          case _          ⇒ false
        }
      case _ ⇒ false
    }

}
