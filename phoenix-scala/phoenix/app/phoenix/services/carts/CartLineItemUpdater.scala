package phoenix.services.carts

import cats.implicits._
import core.db._
import objectframework.services.ObjectManager
import org.json4s.Formats
import phoenix.failures.CartFailures._
import phoenix.failures.ProductFailures.SkuNotFoundForContext
import phoenix.models.account._
import phoenix.models.activity.Activity
import phoenix.models.cord._
import phoenix.models.cord.lineitems.CartLineItems.scope._
import phoenix.models.cord.lineitems._
import phoenix.models.inventory.{IlluminatedSku, Sku, Skus}
import phoenix.models.objects._
import phoenix.models.product.VariantValueSkuLinks
import phoenix.payloads.LineItemPayloads._
import phoenix.responses.TheResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.{CartValidator, LogActivity}
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import slick.jdbc.PostgresProfile.api._

object CartLineItemUpdater {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def updateQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity().orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ updateQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity.some)
    } yield response
  }

  def updateQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity().orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)))

    for {
      cart     ← * <~ finder
      _        ← * <~ updateQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity.some)
    } yield response
  }

  def addQuantitiesOnCart(admin: User, refNum: String, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity().orderLineItemsUpdated(cart, oldQtys, payload, Some(admin))

    for {
      cart     ← * <~ Carts.mustFindByRefNum(refNum)
      _        ← * <~ addQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity.some)
    } yield response
  }

  def addQuantitiesOnCustomersCart(customer: User, payload: Seq[UpdateLineItemsPayload])(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] = {

    val logActivity = (cart: CartResponse, oldQtys: Map[String, Int]) ⇒
      LogActivity().orderLineItemsUpdated(cart, oldQtys, payload)

    val finder = Carts
      .findByAccountId(customer.accountId)
      .one
      .findOrCreate(Carts.create(Cart(accountId = customer.accountId, scope = Scope.current)))

    for {
      cart     ← * <~ finder
      _        ← * <~ addQuantities(cart, payload)
      response ← * <~ runUpdates(cart, logActivity.some)
    } yield response
  }

  def runUpdates(cart: Cart, logAct: Option[(CartResponse, Map[String, Int]) ⇒ DbResultT[Activity]])(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      _ ← * <~ CartPromotionUpdater.readjust(cart, failFatally = false).recover {
           case _ ⇒ () /* FIXME: don’t swallow errors @michalrus */
         }
      cart  ← * <~ CartTotaler.saveTotals(cart)
      valid ← * <~ CartValidator(cart).validate()
      res   ← * <~ CartResponse.buildRefreshed(cart)
      li    ← * <~ CartLineItems.byCordRef(cart.refNum).countSkus
      _     ← * <~ logAct.traverse(_(res, li)).void
    } yield TheResponse.validated(res, valid)

  def foldQuantityPayload(payload: Seq[UpdateLineItemsPayload]): Map[String, Int] =
    payload.foldLeft(Map[String, Int]()) { (acc, item) ⇒
      val quantity = acc.getOrElse(item.sku, 0)
      acc.updated(item.sku, quantity + item.quantity)
    }

  private def updateQuantities(
      cart: Cart,
      payload: Seq[UpdateLineItemsPayload])(implicit ec: EC, ctx: OC, db: DB): DbResultT[Seq[CartLineItem]] =
    for {
      _ ← * <~ CartLineItems
           .byCordRef(cart.referenceNumber)
           .deleteAll(DbResultT.unit, DbResultT.unit)
      updateResult ← * <~ payload.filter(_.quantity > 0).map(updateLineItems(cart, _))
    } yield updateResult.flatten

  private def updateLineItems(cart: Cart,
                              lineItem: UpdateLineItemsPayload)(implicit ec: EC, db: DB, ctx: OC) =
    for {
      sku ← * <~ Skus
             .filterByContext(ctx.id)
             .filter(_.code === lineItem.sku)
             .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, ctx.id))
      fullSku ← ObjectManager.getFullObject(sku.pure[DbResultT])
      _       ← * <~ IlluminatedSku.illuminate(ctx, fullSku).mustBeActive
      // TODO: check if that SKU’s Product is not archived/deactivated @michalrus
      _            ← * <~ mustFindProductIdForSku(sku, cart.refNum)
      updateResult ← * <~ createLineItems(sku.id, lineItem.quantity, cart.refNum, lineItem.attributes)
    } yield updateResult

  private def createLineItems(skuId: Int,
                              quantity: Int,
                              cordRef: String,
                              attributes: Option[LineItemAttributes])(implicit ec: EC) = {
    require(quantity > 0)
    val lineItem = CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes)
    CartLineItems.createAllReturningModels(List.fill(quantity)(lineItem))
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(implicit ec: EC,
                                                                              ctx: OC): DbResultT[Unit] = {
    val lineItemUpdActions = payload.map { lineItem ⇒
      for {
        sku ← * <~ Skus
               .filterByContext(ctx.id)
               .filter(_.code === lineItem.sku)
               .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, ctx.id))
        _ ← * <~ mustFindProductIdForSku(sku, cart.refNum)
        _ ← * <~ (if (lineItem.quantity > 0)
                    createLineItems(sku.id, lineItem.quantity, cart.refNum, lineItem.attributes).meh
                  else
                    removeLineItems(sku.id, -lineItem.quantity, cart.refNum, lineItem.attributes))
      } yield {}
    }
    DbResultT.seqCollectFailures(lineItemUpdActions.toList).meh
  }

  private def mustFindProductIdForSku(sku: Sku, refNum: String)(implicit ec: EC, oc: OC) =
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

  private def removeLineItems(skuId: Int,
                              delta: Int,
                              cordRef: String,
                              requestedAttrs: Option[LineItemAttributes])(implicit ec: EC): DbResultT[Unit] =
    CartLineItems
      .byCordRef(cordRef)
      .filter(_.skuId === skuId)
      .result
      .dbresult
      .flatMap { lineItemsInCart ⇒
        val lisMatchingPayload = lineItemsInCart.filter(_.attributes == requestedAttrs).map(_.id)

        val totalToDelete = Math.min(delta, lisMatchingPayload.length)
        val idsToDelete   = lisMatchingPayload.take(totalToDelete)

        CartLineItems.filter(_.id.inSet(idsToDelete)).deleteAll(DbResultT.unit, DbResultT.unit)
      }
      .meh
}
