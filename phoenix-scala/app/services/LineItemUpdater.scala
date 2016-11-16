package services

import failures.CartFailures._
import failures.ProductFailures.SkuNotFoundForContext
import models.account._
import models.activity.Activity
import models.cord._
import models.cord.lineitems.CartLineItems.scope._
import models.cord.lineitems._
import models.inventory.{Sku, Skus}
import models.objects.{ProductSkuLinks, ProductVariantLinks, VariantValueLinks}
import models.product.VariantValueSkuLinks
import org.json4s.JsonAST.{JNull, JObject}
import payloads.LineItemPayloads.UpdateLineItemsPayload
import responses.TheResponse
import responses.cord.CartResponse
import services.carts.{CartPromotionUpdater, CartTotaler}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object LineItemUpdater {

  def updateQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[Int, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def updateQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[Int, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId)))

    for {
      cart     ← * <~ finder
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[Int, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ addQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[Int, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId)))

    for {
      cart     ← * <~ finder
      _        ← * <~ addQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  private def runUpdates(cart: Cart, logAct: (CartResponse, Map[Int, Int]) ⇒ DbResultT[Activity])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      _     ← * <~ CartPromotionUpdater.readjust(cart).recover { case _ ⇒ Unit }
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      res   ← * <~ CartResponse.buildRefreshed(cart)
      li    ← * <~ CartLineItems.byCordRef(cart.refNum).countSkus
      _     ← * <~ logAct(res, li)
    } yield TheResponse.validated(res, valid)

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[Int, Int] =
    payload.foldLeft(Map[Int, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.skuId, 0)
      acc.updated(item.skuId, quantity + item.quantity)
    }
  private def updateQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload], contextId: Int)(
      implicit ec: EC,
      ctx: OC): DbResultT[Seq[CartLineItem]] = {
    for {
      _ ← * <~ CartLineItems
           .byCordRef(cart.referenceNumber)
           .deleteAll(DbResultT.unit, DbResultT.unit)
      updateResult ← * <~ payload.filter(_.quantity > 0).map(updateLineItems(cart, _, contextId))
    } yield updateResult.flatten
  }

  private def updateLineItems(cart: Cart, lineItem: UpdateLineItemsPayload, contextId: Int)(
      implicit ec: EC,
      ctx: OC) = {
    for {
      sku ← * <~ Skus
             .filterByContext(contextId)
             .filter(_.formId === lineItem.skuId)
             .mustFindOneOr(SkuNotFoundForContext(lineItem.skuId, contextId))
      _            ← * <~ mustFindProductIdForSku(sku, cart.refNum)
      updateResult ← * <~ addLineItems(sku.id, lineItem.quantity, cart.refNum, lineItem.attributes)
    } yield updateResult
  }

  private def addLineItems(skuId: Int, quantity: Int, cordRef: String, attributes: Option[Json])(
      implicit ec: EC) = {
    require(quantity > 0)
    DbResultT.sequence(
        List
          .fill(quantity)(CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes))
          .map(CartLineItems.create)
    )
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      ctx: OC): DbResultT[Seq[Unit]] = {

    val lineItemUpdActions = payload.map { lineItem ⇒
      for {
        sku ← * <~ Skus
               .filterByContext(ctx.id)
               .filter(_.formId === lineItem.skuId)
               .mustFindOneOr(SkuNotFoundForContext(lineItem.skuId, ctx.id))
        _ ← * <~ mustFindProductIdForSku(sku, cart.refNum)
        actionsList ← * <~ (if (lineItem.quantity > 0)
                              addLineItems(sku.id,
                                           lineItem.quantity,
                                           cart.refNum,
                                           lineItem.attributes).meh
                            else
                              removeLineItems(sku.id,
                                              -lineItem.quantity,
                                              cart.refNum,
                                              lineItem.attributes))
      } yield actionsList
    }
    DbResultT.sequence(lineItemUpdActions).map { actions ⇒
      actions.map(_ ⇒ ())
    }
  }

  private def mustFindProductIdForSku(sku: Sku, refNum: String)(implicit ec: EC, oc: OC) = {
    for {
      link ← * <~ ProductSkuLinks.filter(_.rightId === sku.id).one.dbresult.flatMap {
              case Some(productLink) ⇒
                DbResultT.good(productLink.leftId)
              case None ⇒
                for {
                  valueLink ← * <~ VariantValueSkuLinks
                               .filter(_.rightId === sku.id)
                               .mustFindOneOr(SkuWithNoProductAdded(refNum, sku.code))
                  variantLink ← * <~ VariantValueLinks
                                 .filter(_.rightId === valueLink.leftId)
                                 .mustFindOneOr(SkuWithNoProductAdded(refNum, sku.code))
                  productLink ← * <~ ProductVariantLinks
                                 .filter(_.rightId === variantLink.leftId)
                                 .mustFindOneOr(SkuWithNoProductAdded(refNum, sku.code))
                } yield productLink.leftId
            }
    } yield link
  }

  private def removeLineItems(skuId: Int, delta: Int, cordRef: String, attributes: Option[Json])(
      implicit ec: EC): DbResultT[Unit] = {
    CartLineItems
      .byCordRef(cordRef)
      .filter(_.skuId === skuId)
      .result
      .flatMap { lineItems ⇒
        val itemsWithSameAttributes = filterLineItemsByAttributes(lineItems, attributes).map(_.id)
        val totalToDelete           = Math.min(delta, itemsWithSameAttributes.length)
        val idsToDelete             = itemsWithSameAttributes.take(totalToDelete)
        CartLineItems.filter(_.id inSet idsToDelete).delete
      }
      .dbresult
      .meh
  }

  private def filterLineItemsByAttributes(lineItems: Seq[CartLineItem],
                                          presentAttributes: Option[Json]) = {
    lineItems.filter { li ⇒
      (presentAttributes, li.attributes) match {
        case (Some(p), Some(a))            ⇒ p.equals(a)
        case (None, Some(a: JNull.type))   ⇒ true
        case (None, Some(JObject(fields))) ⇒ fields.isEmpty
        case (None, None)                  ⇒ true
        case _                             ⇒ false
      }
    }
  }
}
