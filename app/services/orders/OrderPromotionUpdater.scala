package services.orders

import cats.data.Xor
import cats.implicits._
import concepts.discounts.{OfferAstCompiler, QualifierAstCompiler}
import concepts.discounts.qualifiers._
import concepts.discounts.offers._
import failures.OrderFailures._
import failures.CouponFailures._
import failures.PromotionFailures._
import failures.GeneralFailure
import models.discount.IlluminatedDiscount
import models.discount.IlluminatedDiscount.illuminate
import models.objects._
import models.order._
import models.order.OrderPromotions.scope._
import models.order.lineitems._
import models.coupon._
import models.promotion._
import models.promotion.Promotions.scope._
import models.shipping
import models.traits.Originator
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods._
import responses.order.FullOrder._
import services.Result
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

/*
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.sksamuel.elastic4s.ElasticDsl._
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.search.aggregations.bucket.filter.InternalFilter
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
*/

object OrderPromotionUpdater {

  /*
  def elasticTest()(implicit ec: EC): Result[String] = {
    val uri = "elasticsearch://10.240.0.8:9300"

    val settings = Settings.settingsBuilder().put("cluster.name", "elasticsearch").build()
    val client = ElasticClient.transport(settings, ElasticsearchClientUri(uri))

    //val rawJson = """{"bool": {"filter": [{"range": {"subTotal": {"gte": 10000}}}]}}"""
    val rawJson = """{"query":{"bool":{"filter":[{"range":{"subTotal":{"gte":10000}}}]}}}"""
    val refList = Seq("BR10387", "BR10022", "BR10566").toList

    val req1 = search in "phoenix/orders_search_view" rawQuery rawJson aggregations(
      aggregation filter "qualifier" filter termsQuery("referenceNumber", refList: _*)
    ) size 0

    try {
      val future = client.execute(req1)
      Console.println(req1.show)

      Result.fromFuture(future.map { response ⇒
        val qualifierOpt = response.aggregations.getAsMap.asScala.get("qualifier")
        qualifierOpt match {
          case Some(q) ⇒ q.asInstanceOf[InternalFilter].getDocCount.toString
          case _       ⇒ "0"
        }
      })
    } catch {
      case NonFatal(e) ⇒
        Result.failure(GeneralFailure(e.getMessage))
    }
  }
  */

  def attachCoupon(originator: Originator, refNum: Option[String] = None, context: ObjectContext, code: String)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    // Fetch base data
    order             ← * <~ getCartByOriginator(originator, refNum)
    _                 ← * <~ order.mustBeCart
    orderPromotions   ← * <~ OrderPromotions.filterByOrderId(order.id).requiresCoupon.one.
      mustNotFindOr(OrderAlreadyHasCoupon)
    // Fetch coupon + validate
    couponCode        ← * <~ CouponCodes.mustFindByCode(code)
    coupon            ← * <~ Coupons.filterByContextAndFormId(context.id, couponCode.couponFormId).one.
      mustFindOr(CouponWithCodeCannotBeFound(code))
    couponForm        ← * <~ ObjectForms.mustFindById404(coupon.formId)
    couponShadow      ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    couponObject      = IlluminatedCoupon.illuminate(context, coupon, couponForm, couponShadow)
    _                 ← * <~ couponObject.mustBeActive
    // Fetch promotion + validate
    promotion         ← * <~ Promotions.filterByContextAndFormId(context.id, coupon.promotionId).requiresCoupon.one.
      mustFindOr(PromotionNotFoundForContext(coupon.promotionId, context.name))
    promoForm         ← * <~ ObjectForms.mustFindById404(promotion.formId)
    promoShadow       ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
    promoObject       = IlluminatedPromotion.illuminate(context, promotion, promoForm, promoShadow)
    _                 ← * <~ promoObject.mustBeActive
    // Fetch discount
    discountLinks     ← * <~ ObjectLinks.filter(_.leftId === promoShadow.id).result
    discountShadowIds = discountLinks.map(_.rightId)
    discountShadows   ← * <~ ObjectShadows.filter(_.id.inSet(discountShadowIds)).result
    discountFormIds   = discountShadows.map(_.formId)
    discountForms     ← * <~ ObjectForms.filter(_.id.inSet(discountFormIds)).result
    discountsTupled   = for (f ← discountForms; s ← discountShadows if s.formId == f.id) yield (s, f)
    discounts         = discountsTupled.map { case (shad, form) ⇒ illuminate(context.some, form, shad) }
    // Safe AST compilation
    discount          ← * <~ tryDiscount(discounts)
    qualifier         ← * <~ tryQualifier(discount)
    offer             ← * <~ tryOffer(discount)
    adjustments       ← * <~ getAdjustments(order, promoShadow.id, qualifier, offer)
    // Create connected promotion and line item adjustments
    _                 ← * <~ OrderPromotions.create(OrderPromotion.buildCoupon(order, promotion, couponCode))
    _                 ← * <~ OrderLineItemAdjustments.createAll(adjustments)
    // Response
    order             ← * <~ OrderTotaler.saveTotals(order)
    response          ← * <~ refreshAndFullOrder(order).toXor
  } yield response).runTxn()

  def detachCoupon(originator: Originator, refNum: Option[String] = None)
    (implicit ec: EC, db: DB): Result[Root] = (for {
    // Read
    order           ← * <~ getCartByOriginator(originator, refNum)
    _               ← * <~ order.mustBeCart
    orderPromotions ← * <~ OrderPromotions.filterByOrderId(order.id).requiresCoupon.result
    shadowIds       = orderPromotions.map(_.promotionShadowId)
    promotions      ← * <~ Promotions.filter(_.shadowId.inSet(shadowIds)).requiresCoupon.result
    deleteShadowIds = promotions.map(_.shadowId)
    // Write
    _               ← * <~ OrderPromotions.filterByOrderIdAndShadows(order.id, deleteShadowIds).delete
    _               ← * <~ OrderLineItemAdjustments.filterByOrderIdAndShadows(order.id, deleteShadowIds).delete
    _               ← * <~ OrderTotaler.saveTotals(order)
    response        ← * <~ refreshAndFullOrder(order).toXor
  } yield response).runTxn()

  private def tryDiscount(discounts: Seq[IlluminatedDiscount]) = discounts.headOption match {
    case Some(discount) ⇒ Xor.Right(discount)
    case _              ⇒ Xor.Left(GeneralFailure("Invalid discount object for promotion").single) // FIXME
  }

  private def tryQualifier(discount: IlluminatedDiscount) = discount.attributes \ "qualifier" \ "v" match {
    case JObject(o) ⇒ QualifierAstCompiler(compact(render(JObject(o)))).compile()
    case _          ⇒ Xor.Left(GeneralFailure("Invalid qualifier object for promotion").single) // FIXME
  }

  private def tryOffer(discount: IlluminatedDiscount) = discount.attributes \ "offer" \ "v" match {
    case JObject(o) ⇒ OfferAstCompiler(compact(render(JObject(o)))).compile()
    case _          ⇒ Xor.Left(GeneralFailure("Invalid offer object for promotion").single) // FIXME
  }

  private def getAdjustments(order: Order, promoId: Int, qualifier: Qualifier, offer: Offer)
    (implicit ec: EC, db: DB) = for {
    orderDetails                ← * <~ fetchOrderDetails(order).toXor
    (lineItems, shippingMethod) = orderDetails
    _                           ← * <~ qualifier.check(order, lineItems, shippingMethod)
    lineItemAdjustments         ← * <~ offer.adjust(order, promoId, lineItems, shippingMethod)
  } yield lineItemAdjustments

  private def fetchOrderDetails(order: Order)(implicit ec: EC) = for {
    lineItemTup ← OrderLineItemSkus.findLineItemsByOrder(order).result
    lineItems   = lineItemTup.map {
      case (sku, skuForm, skuShadow, lineItem) ⇒
        OrderLineItemProductData(sku, skuForm, skuShadow, lineItem)
    }
    shipMethod  ← shipping.ShippingMethods.forOrder(order).one
  } yield (lineItems, shipMethod)
}
