package services

import failures.ProductFailures.SkuNotFoundForContext
import models.StoreAdmin
import models.activity.Activity
import models.cord._
import models.cord.lineitems.OrderLineItems.scope._
import models.cord.lineitems._
import CartLineItems.scope._
import failures.CartFailures.SKUWithNoProductAdded
import failures.GeneralFailure
import models.customer.Customer
import models.inventory.Skus
import models.objects.ProductSkuLinks
import models.payment.giftcard._
import org.json4s.JsonAST.{JNull}
import payloads.LineItemPayloads.UpdateLineItemsPayload
import responses.TheResponse
import responses.cord.CartResponse
import services.carts.{CartPromotionUpdater, CartTotaler}
import slick.dbio.DBIOAction
import slick.driver.PostgresDriver.api._
import utils.aliases
import utils.aliases._
import utils.db._

import scala.reflect.internal.util.Statistics.Quantity

object LineItemUpdater {

  def updateQuantitiesOnCart(admin: StoreAdmin,
                             refNum: String,
                             payload: Seq[UpdateLineItemsPayload])(
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

  def updateLineItemsOnCustomersCart(customer: Customer, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder =
      Carts.findByCustomer(customer).one.findOrCreate(Carts.create(Cart(customerId = customer.id)))

    for {
      cart     ← * <~ finder
      _        ← * <~ updateQuantities(cart, payload, ctx.id)
      response ← * <~ runUpdates(cart, logActivity)
    } yield response
  }

  def addQuantitiesOnCart(admin: StoreAdmin, refNum: String, payload: Seq[UpdateLineItemsPayload])(
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

  def addQuantitiesOnCustomersCart(customer: Customer, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      es: ES,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity.orderLineItemsUpdated(cart, oldQtys, payload)

    val finder =
      Carts.findByCustomer(customer).one.findOrCreate(Carts.create(Cart(customerId = customer.id)))

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
             .mustFindOneOr(
                 SkuNotFoundForContext(lineItem.sku, contextId)
             )
      _ ← * <~ ProductSkuLinks
           .filter(_.rightId === sku.id)
           .mustFindOneOr(SKUWithNoProductAdded(cart.refNum, lineItem.sku))
      updateAction ← * <~ addLineItem(sku.id, cart.refNum, lineItem.quantity, lineItem.attributes)
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

    val lineItemUpdActions = payload.map { lineItem ⇒
      for {
        sku ← * <~ Skus
               .filterByContext(ctx.id)
               .filter(_.code === lineItem.sku)
               .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, ctx.id))
        _ ← * <~ ProductSkuLinks
             .filter(_.rightId === sku.id)
             .mustFindOneOr(SKUWithNoProductAdded(cart.refNum, lineItem.sku))
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

  /** private def doUpdateLineItems(skuId: Int, newQuantity: Int, cordRef: String,attributes:Option[Json])(
      implicit ec: EC): DbResultT[Seq[CartLineItem]] =
    for {
      current ← * <~ CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId).size.result
      _ ← * <~ (if (newQuantity > current)
                  increaseLineItems(skuId, newQuantity - current, cordRef,attributes)
                else decreaseLineItems(skuId, current - newQuantity, cordRef))
      lineItems ← * <~ CartLineItems.byCordRef(cordRef).result
    } yield lineItems**/
  private def increaseLineItems(skuId: Int, delta: Int, cordRef: String, attributes: Option[Json])(
      implicit ec: EC): DbResultT[Unit] = {
    val itemsToInsert: List[CartLineItem] =
      List.fill(delta)(CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes))
    val a = for {
      a ← * <~ CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId)
      _ ← * <~ println("amount of lineItems with sku " + skuId + "  " + a.length)

    } yield {}
    CartLineItems.createAll(itemsToInsert).meh
  }

  private def decreaseLineItems(skuId: Int, delta: Int, cordRef: String, attributes: Option[Json])(
      implicit ec: EC): DbResultT[Unit] = {
    for {
      _ ← * <~ CartLineItems.byCordRef(cordRef).filter(_.skuId === skuId).result.flatMap {
           lineItems ⇒
             val matchedLineItems = lineItemAttributesComparison(lineItems, attributes).map(_.id)
             println("matchLineItems size " + matchedLineItems.length)
             val itemsToDelete =
               CartLineItems.byCordRef(cordRef).filter(cli ⇒ cli.id.inSet(matchedLineItems))
             if (delta < matchedLineItems.length)
               itemsToDelete.filter(_.id in itemsToDelete.take(delta).map(_.id)).delete
             else if (delta > matchedLineItems.length && matchedLineItems.length != 0)
               itemsToDelete
                 .filter(_.id in itemsToDelete.take(matchedLineItems.length).map(_.id))
                 .delete
             else
               DBIOAction.successful(0)
         }

    } yield {}
  }

  private def lineItemAttributesComparison(lineItems: Seq[CartLineItem],
                                           presentAttributes: Option[Json]) = {
    lineItems.filter { lI ⇒
      (presentAttributes, lI.attributes) match {
        case (Some(p), Some(a))          ⇒ println("is where it should be " + p + "   " + a); p == a
        case (None, Some(a: JNull.type)) ⇒ true
        case (None, None)                ⇒ true
        case (a, b)                      ⇒ false
      }
    }
  }
}
