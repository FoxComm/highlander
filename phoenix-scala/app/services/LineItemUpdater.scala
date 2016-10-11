package services

import failures.ProductFailures.SkuNotFoundForContext
import models.activity.Activity
import models.cord._
import models.cord.lineitems.OrderLineItems.scope._
import models.cord.lineitems._
import CartLineItems.scope._
import failures.CartFailures.SKUWithNoProductAdded
import failures.GeneralFailure
import models.account._
import models.inventory.{Sku, Skus}
import models.objects.{ProductSkuLinks, ProductVariantLinks, VariantValueLinks}
import models.payment.giftcard._
import models.product.VariantValueSkuLinks
import payloads.LineItemPayloads.UpdateLineItemsPayload
import responses.TheResponse
import responses.cord.CartResponse
import services.carts.{CartPromotionUpdater, CartTotaler}
import slick.driver.PostgresDriver.api._
import utils.aliases
import utils.aliases._
import utils.db._

import scala.reflect.internal.util.Statistics.Quantity

object LineItemUpdater {

  def updateQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
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

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
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

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
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

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
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

  private def runUpdates(cart: Cart,
                         logAct: (CartResponse, Map[String, Int]) ⇒ DbResultT[Activity])(
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

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[String, Int] =
    payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

  private def updateQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload], contextId: Int)(
      implicit ec: EC): DbResultT[Seq[CartLineItem]] = {
    for {
      itemsDeleted ← * <~ CartLineItems.byCordRef(cart.referenceNumber).delete
      upAc ← * <~ payload
              .filter(lI ⇒ lI.quantity > 0)
              .map(lineItem ⇒ updateLineItems(cart, lineItem, contextId))
      flatList ← * <~ upAc.flatten
    } yield flatList
  }

  private def updateLineItems(cart: Cart, lineItem: UpdateLineItemsPayload, contextId: Int)(
      implicit ec: EC) = {
    for {
      sku ← * <~ Skus
             .filterByContext(contextId)
             .filter(_.code === lineItem.sku)
             .mustFindOneOr({
               println("===============> lineItemSku: " + lineItem.sku);
               SkuNotFoundForContext(lineItem.sku, contextId)
             })
      _ ← * <~ ProductSkuLinks
           .filter(_.rightId === sku.id)
           .mustFindOneOr(SKUWithNoProductAdded(cart.refNum, lineItem.sku))
      updateAction ← * <~ addLineItem(sku.id, cart.refNum, lineItem.quantity, lineItem.attributes)
      _            ← * <~ DbResultT.good(println(sku))
    } yield updateAction
  }

  private def addLineItem(skuId: Int, cordRef: String, quantity: Int, attributes: Option[Json])(
      implicit ec: EC) = {

    val itemsToAdd =
      List.fill(quantity)(CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes))
    itemsToAdd.map(cli ⇒ CartLineItems.create(cli))
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      ctx: OC): DbResultT[Seq[Unit]] = {

    val lineItemUpdActions = foldQuantityPayload(payload).map {
      case (skuCode, delta) ⇒
        for {
          sku ← * <~ Skus
                 .filterByContext(ctx.id)
                 .filter(_.code === skuCode)
                 .mustFindOneOr(SkuNotFoundForContext(skuCode, ctx.id))
          _ ← * <~ mustFindProductIdForSku(sku, cart.refNum)
          lis ← * <~ (if (delta > 0) increaseLineItems(sku.id, delta, cart.refNum)
                      else decreaseLineItems(sku.id, -delta, cart.refNum))
        } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.toSeq)
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
                               .mustFindOneOr(SKUWithNoProductAdded(refNum, sku.code))
                  variantLink ← * <~ VariantValueLinks
                                 .filter(_.rightId === valueLink.leftId)
                                 .mustFindOneOr(SKUWithNoProductAdded(refNum, sku.code))
                  productLink ← * <~ ProductVariantLinks
                                 .filter(_.rightId === variantLink.leftId)
                                 .mustFindOneOr(SKUWithNoProductAdded(refNum, sku.code))
                } yield productLink.leftId
            }
    } yield link
  }

  private def doUpdateLineItems(skuId: Int, newQuantity: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Seq[CartLineItem]] =
    for {
      current ← * <~ CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId).size.result
      _ ← * <~ (if (newQuantity > current)
                  increaseLineItems(skuId, newQuantity - current, cordRef)
                else decreaseLineItems(skuId, current - newQuantity, cordRef))
      lineItems ← * <~ CartLineItems.byCordRef(cordRef).result
    } yield lineItems

  private def increaseLineItems(skuId: Int, delta: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Unit] = {

    val itemsToInsert: List[CartLineItem] =
      List.fill(delta)(CartLineItem(cordRef = cordRef, skuId = skuId, attributes = None))
    CartLineItems.createAll(itemsToInsert).meh
  }

  private def decreaseLineItems(skuId: Int, delta: Int, cordRef: String)(
      implicit ec: EC): DbResultT[Unit] = {

    val items = CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId)
    items.filter(_.id in items.take(delta).map(_.id)).deleteAll(DbResultT.unit, DbResultT.unit)
  }
}
