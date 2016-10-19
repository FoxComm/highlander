package services

import failures.ProductFailures.SkuNotFoundForContext
import models.activity.Activity
import models.cord._
import models.cord.lineitems.OrderLineItems.scope._
import models.cord.lineitems._
import CartLineItems.scope._
import failures.CartFailures.SkuWithNoProductAdded
import failures.GeneralFailure
import models.account._
import models.inventory.Skus
import models.objects.ProductSkuLinks
import models.payment.giftcard._
import org.json4s.JsonAST.JNull
import payloads.LineItemPayloads.UpdateLineItemsPayload
import responses.TheResponse
import responses.cord.CartResponse
import services.carts.{CartPromotionUpdater, CartTotaler}
import slick.dbio.DBIOAction
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

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def updateLineItemsOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
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
      _ ← CartLineItems.byCordRef(cart.referenceNumber).deleteAll(DbResultT.unit, DbResultT.unit)
      updateActions ← * <~ payload
                       .filter(_.quantity > 0)
                       .map(lineItem ⇒ updateLineItems(cart, lineItem, contextId))
    } yield updateActions.flatten
  }

  private def updateLineItems(cart: Cart, lineItem: UpdateLineItemsPayload, contextId: Int)(
      implicit ec: EC) =
    for {
      sku ← * <~ Skus
             .filterByContextAndCode(contextId, lineItem.sku)
             .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, contextId))
      _ ← * <~ ProductSkuLinks
           .filter(_.rightId === sku.id)
           .mustFindOneOr(SkuWithNoProductAdded(cart.refNum, lineItem.sku))
      updateResult ← * <~ addLineItem(sku.id, cart.refNum, lineItem.quantity, lineItem.attributes)
    } yield updateResult

  private def addLineItem(skuId: Int, cordRef: String, quantity: Int, attributes: Option[Json])(
      implicit ec: EC) = {
    val newLineItem = CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes)
    val itemsToAdd  = List.fill(quantity)(newLineItem)
    itemsToAdd.map(CartLineItems.create)
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      ctx: OC): DbResultT[Seq[Unit]] = {

    val lineItemUpdActions = payload.map { lineItem ⇒
      for {
        sku ← * <~ Skus
               .filterByContextAndCode(ctx.id, lineItem.sku)
               .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, ctx.id))
        _ ← * <~ ProductSkuLinks
             .filter(_.rightId === sku.id)
             .mustFindOneOr(SkuWithNoProductAdded(cart.refNum, lineItem.sku))
        lis ← * <~ (if (lineItem.quantity > 0)
                      increaseLineItems(sku.id,
                                        lineItem.quantity,
                                        cart.refNum,
                                        lineItem.attributes)
                    else
                      decreaseLineItems(sku.id,
                                        -lineItem.quantity,
                                        cart.refNum,
                                        lineItem.attributes))
      } yield lis
    }
    DbResultT.sequence(lineItemUpdActions).map(_.toSeq)
  }

  private def increaseLineItems(skuId: Int, delta: Int, cordRef: String, attributes: Option[Json])(
      implicit ec: EC): DbResultT[Unit] = {
    val itemsToInsert: List[CartLineItem] =
      List.fill(delta)(CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes))
    CartLineItems.createAll(itemsToInsert).meh
  }

  private def decreaseLineItems(skuId: Int, delta: Int, cordRef: String, attributes: Option[Json])(
      implicit ec: EC): DbResultT[Unit] = {

    CartLineItems
      .byCordRef(cordRef)
      .filter(_.skuId === skuId)
      .result
      .flatMap { lineItems ⇒
        val itemsWithSameAtributtes =
          lineItemJsonAttributesComparison(lineItems, attributes).map(_.id)
        val itemsToDelete =
          CartLineItems.byCordRef(cordRef).filter(_.id.inSet(itemsWithSameAtributtes))
        if (delta < itemsWithSameAtributtes.length)
          itemsToDelete.filter(_.id in itemsToDelete.take(delta).map(_.id)).delete
        else if (delta > itemsWithSameAtributtes.length && itemsWithSameAtributtes.length != 0)
          itemsToDelete
            .filter(_.id in itemsToDelete.take(itemsWithSameAtributtes.length).map(_.id))
            .delete
        else
          DBIOAction.successful(0)
      }
      .dbresult
      .meh
  }

  private def lineItemJsonAttributesComparison(lineItems: Seq[CartLineItem],
                                               presentAttributes: Option[Json]) = {
    lineItems.filter { li ⇒
      (presentAttributes, li.attributes) match {
        case (Some(p), Some(a))          ⇒ p == a
        case (None, Some(a: JNull.type)) ⇒ true
        case (None, None)                ⇒ true
        case _                           ⇒ false
      }
    }
  }
}
