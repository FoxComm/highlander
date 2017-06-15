package phoenix.services.carts

import cats.implicits._
import core.db._
import core.failures.GeneralFailure
import objectframework.models._
import org.json4s.Formats
import phoenix.failures.CartFailures.SkuWithNoProductAdded
import phoenix.models.account._
import phoenix.models.activity.Activity
import phoenix.models.cord._
import phoenix.models.cord.lineitems.CartLineItems.scope._
import phoenix.models.cord.lineitems._
import phoenix.models.inventory._
import phoenix.models.product._
import phoenix.payloads.LineItemPayloads._
import phoenix.queries.ProductSkuQueries.productSkuFormsQuery
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

  private def updateQuantities(cart: Cart,
                               payload: Seq[UpdateLineItemsPayload])(implicit ec: EC, ctx: OC, db: DB) =
    for {
      _ ← * <~ CartLineItems.byCordRef(cart.referenceNumber).delete
      // TODO filter by context
      skusProducts ← * <~ productSkuFormsQuery.filter {
                      case (_, (sku, _, _), _) ⇒ sku.code inSet payload.map(_.sku)
                    }.result
      skus ← * <~ skusProducts.map {
              case (_, (sku, skuForm, skuShadow), maybeProductFiSH) ⇒
                DbResultT.fromEither(for {
                  _ ← IlluminatedSku.illuminate(ctx, FullObject(sku, skuForm, skuShadow)).mustBeActive
                  _ ← maybeProductFiSH match {
                       case Some((product, productForm, productShadow)) ⇒
                         IlluminatedProduct.illuminate(ctx, product, productForm, productShadow).mustBeActive
                       case _ ⇒
                         Either.left(SkuWithNoProductAdded(cart.refNum, sku.code).single)
                     }
                } yield sku)
            }
      _ ← * <~ failIf(
           skus.length != payload.length,
           GeneralFailure(
             "Something went terribly wrong, one of requested SKUs sucks like an industrial-grade vacuum cleaner"))
      newLiMetas = for {
        existingSku ← skus
        requested   ← payload if requested.sku == existingSku.code
      } yield {
        NewLiMeta(skuId = existingSku.id,
                  cordRef = cart.referenceNumber,
                  requestedQuantity = requested.quantity,
                  attributes = requested.attributes)
      }
      _ ← * <~ createLineItems(newLiMetas)
    } yield {}

  private case class NewLiMeta(skuId: Int,
                               cordRef: String,
                               requestedQuantity: Int,
                               attributes: Option[LineItemAttributes])

  private def createLineItems(newLiMetas: List[NewLiMeta])(implicit ec: EC) = {
    val allNewLis = newLiMetas.flatMap { liMeta ⇒
      import liMeta._
      List.fill(requestedQuantity)(CartLineItem(cordRef = cordRef, skuId = skuId, attributes = attributes))
    }
    CartLineItems.createAll(allNewLis)
  }

  private def addQuantities(cart: Cart, payload: Seq[UpdateLineItemsPayload])(implicit ec: EC,
                                                                              ctx: OC): DbResultT[Unit] = ???
//  {
//    val lineItemUpdActions = payload.map { lineItem ⇒
//      for {
//        sku ← * <~ Skus
//               .filterByContext(ctx.id)
//               .filter(_.code === lineItem.sku)
//               .mustFindOneOr(SkuNotFoundForContext(lineItem.sku, ctx.id))
//        _ ← * <~ (if (lineItem.quantity > 0)
//                    createLineItems(sku.id, lineItem.quantity, cart.refNum, lineItem.attributes).meh
//                  else
//                    removeLineItems(sku.id, -lineItem.quantity, cart.refNum, lineItem.attributes))
//      } yield {}
//    }
//    DbResultT.seqCollectFailures(lineItemUpdActions.toList).meh
//  }

  def theQ(cartRef: String)(implicit ctx: OC) = {
    // left joining here because line item can not yet exist for requested SKU
    val q = (productSkuFormsQuery joinLeft CartLineItems)
      .on { case ((skuId, _, _), lineItem) ⇒ skuId === lineItem.skuId }
      .filter { case (_, lineItem) ⇒ lineItem.map(_.cordRef === cartRef).getOrElse(true) }
      .map { case ((_, skuFiSH, productFiSH), lineItem) ⇒ (lineItem, skuFiSH, productFiSH) }

    q.result
  }

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
