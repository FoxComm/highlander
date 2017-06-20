package phoenix.services.carts

import cats._
import cats.implicits._
import core.db._
import core.failures.Failures
import objectframework.models._
import org.json4s.JsonAST._
import phoenix.failures.CouponFailures._
import phoenix.failures.DiscountCompilerFailures._
import phoenix.failures.OrderFailures._
import phoenix.failures.PromotionFailures._
import phoenix.failures.UserFailures.UserWithAccountNotFound
import phoenix.models.account.{User, Users}
import phoenix.models.cord.OrderPromotions.scope._
import phoenix.models.cord._
import phoenix.models.cord.lineitems._
import phoenix.models.coupon._
import phoenix.models.discount.DiscountHelpers._
import phoenix.models.discount._
import phoenix.models.discount.offers._
import phoenix.models.discount.qualifiers._
import phoenix.models.objects.PromotionDiscountLinks
import phoenix.models.promotion.Promotions.scope._
import phoenix.models.promotion._
import phoenix.models.shipping
import phoenix.responses.TheResponse
import phoenix.responses.cord.CartResponse
import phoenix.services.customerGroups.GroupMemberManager
import phoenix.services.discount.compilers._
import phoenix.services.{CartValidator, LineItemManager, LogActivity}
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import phoenix.utils.apis.Apis
import slick.jdbc.PostgresProfile.api._

object CartPromotionUpdater {

  def readjust(cart: Cart, failFatally: Boolean /* FIXME with the new foxy monad @michalrus */ )(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ctx: OC,
      au: AU): DbResultT[Unit] =
    tryReadjust(cart, failFatally).recoverWith {
      case es if es exists OrderHasNoPromotions.== ⇒
        clearStalePromotions(cart) >> DbResultT.failures(es)
    }

  private def tryReadjust(cart: Cart, failFatally: Boolean /* FIXME with the new foxy monad @michalrus */ )(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ctx: OC,
      au: AU): DbResultT[Unit] =
    for {
      // Fetch base stuff
      oppa ← * <~ findApplicablePromotion(cart, failFatally)
      (orderPromo, promotion, adjustments) = oppa // 🙄
      // Delete previous adjustments and create new
      _ ← * <~ CartLineItemAdjustments.findByCordRef(cart.refNum).delete
      _ ← * <~ CartLineItemAdjustments.createAll(adjustments)
    } yield ()

  private def clearStalePromotions(cart: Cart)(implicit ec: EC): DbResultT[Unit] =
    OrderPromotions.filterByCordRef(cart.refNum).delete.dbresult.void >>
      CartLineItemAdjustments.findByCordRef(cart.referenceNumber).delete.dbresult.void

  private def filterPromotionsUsingCustomerGroups[L[_]: TraverseFilter](user: User)(
      promos: L[Promotion])(implicit ec: EC, apis: Apis, ctx: OC, db: DB): DbResultT[L[Promotion]] = {
    implicit val formats = JsonFormatters.phoenixFormats

    def isApplicable(promotion: Promotion): DbResultT[Boolean] =
      for {
        promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
        promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
        promoObject = IlluminatedPromotion.illuminate(ctx, promotion, promoForm, promoShadow)
        customerGroupIdsO = promoObject.attributes \ "customerGroupIds" \ "v" match {
          case JNothing | JNull ⇒ None
          case cgis             ⇒ cgis.extractOpt[Set[Int]]
        }
        result ← * <~ customerGroupIdsO.fold(DbResultT.pure(true))(GroupMemberManager.isMemberOfAny(_, user))
      } yield result

    promos.filterA(isApplicable)
  }

  private def findApplicablePromotion(cart: Cart,
                                      failFatally: Boolean /* FIXME with the new foxy monad @michalrus */ )(
      implicit ec: EC,
      apis: Apis,
      au: AU,
      db: DB,
      ctx: OC): DbResultT[(OrderPromotion, Promotion, Seq[CartLineItemAdjustment])] =
    findAppliedCouponPromotion(cart, failFatally).handleErrorWith(
      couponErr ⇒
        findApplicableAutoAppliedPromotion(cart).handleErrorWith(_ ⇒ // Any error? @michalrus
          DbResultT.failures(couponErr)))

  private def findAppliedCouponPromotion(
      cart: Cart,
      failFatally: Boolean /* FIXME with the new foxy monad @michalrus */ )(
      implicit ec: EC,
      au: AU,
      apis: Apis,
      db: DB,
      ctx: OC): DbResultT[(OrderPromotion, Promotion, Seq[CartLineItemAdjustment])] =
    for {
      orderPromo ← * <~ OrderPromotions
                    .filterByCordRef(cart.refNum)
                    .couponOnly
                    .mustFindOneOr(OrderHasNoPromotions)
      user ← * <~ Users
              .findOneByAccountId(cart.accountId)
              .mustFindOr(UserWithAccountNotFound(cart.accountId))
      promotionsQ = Promotions
        .filterByContextAndShadowId(ctx.id, orderPromo.promotionShadowId)
        .filter(_.archivedAt.isEmpty)
        .couponOnly
      promotions ← * <~ promotionsQ.result.dbresult
                    .map(_.toStream) >>= filterPromotionsUsingCustomerGroups(user)
      promotion ← * <~ promotions.headOption
                   .map(DbResultT.pure(_))
                   .getOrElse(DbResultT.failure(OrderHasNoPromotions)) // TODO: no function for that? Seems useful? @michalrus
      adjustments ← * <~ getAdjustmentsForPromotion(cart, promotion, failFatally)
    } yield (orderPromo, promotion, adjustments)

  private def findApplicableAutoAppliedPromotion(cart: Cart)(
      implicit ec: EC,
      apis: Apis,
      au: AU,
      db: DB,
      ctx: OC): DbResultT[(OrderPromotion, Promotion, Seq[CartLineItemAdjustment])] =
    for {
      user ← * <~ Users
              .findOneByAccountId(cart.accountId)
              .mustFindOr(UserWithAccountNotFound(cart.accountId))
      promotionsQ = Promotions.filterByContext(ctx.id).filter(_.archivedAt.isEmpty).autoApplied
      all ← * <~ promotionsQ.result.dbresult
             .map(_.toStream) >>= filterPromotionsUsingCustomerGroups(user)
      allWithAdjustments ← * <~ all.toList
                            .map(promo ⇒
                              getAdjustmentsForPromotion(cart, promo, failFatally = true).map((promo, _)))
                            .ignoreFailures
                            .ensure(OrderHasNoPromotions.single)(_.nonEmpty)
      (bestPromo, bestAdjustments) = allWithAdjustments.maxBy {
        case (_, adjustments) ⇒ adjustments.map(_.subtract).sum
      } // FIXME: This approach doesn’t seem very efficient… @michalrus
      // Replace previous OrderPromotions bindings with the current best one.
      // TODO: only if they differ?
      _          ← * <~ OrderPromotions.filterByCordRef(cart.refNum).autoApplied.delete
      orderPromo ← * <~ OrderPromotions.create(OrderPromotion.buildAuto(cart, bestPromo))
    } yield (orderPromo, bestPromo, bestAdjustments)

  private def getAdjustmentsForPromotion(
      cart: Cart,
      promotion: Promotion,
      failFatally: Boolean /* FIXME with the new foxy monad @michalrus */ )(
      implicit ec: EC,
      apis: Apis,
      au: AU,
      db: DB,
      ctx: OC): DbResultT[Seq[CartLineItemAdjustment]] =
    for {
      // Fetch promotion
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
      promoObject = IlluminatedPromotion.illuminate(ctx, promotion, promoForm, promoShadow)
      _         ← * <~ promoObject.mustBeActive
      discounts ← * <~ PromotionDiscountLinks.queryRightByLeft(promotion)
      // Safe AST compilation
      discount ← * <~ tryDiscount(discounts)
      (form, shadow) = discount.tupled
      qualifier ← * <~ QualifierAstCompiler(qualifier(form, shadow)).compile()
      offer     ← * <~ OfferAstCompiler(offer(form, shadow)).compile()
      maybeFailedAdjustments = getAdjustments(promoShadow, cart, qualifier, offer)
      adjustments ← * <~ (if (failFatally) maybeFailedAdjustments
                          else
                            (maybeFailedAdjustments
                              .failuresToWarnings(Seq.empty) { case _ ⇒ true }))
    } yield adjustments

  def attachCoupon(originator: User, refNum: Option[String] = None, code: String)(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ac: AC,
      ctx: OC,
      au: AU): DbResultT[TheResponse[CartResponse]] =
    for {
      // Fetch base data
      cart ← * <~ getCartByOriginator(originator, refNum)
      _ ← * <~ OrderPromotions
           .filterByCordRef(cart.refNum)
           .couponOnly // TODO: decide what happens here, when we allow multiple promos per cart. @michalrus
           .mustNotFindOneOr(OrderAlreadyHasCoupon)
      // Fetch coupon + validate
      couponCode ← * <~ CouponCodes.mustFindByCode(code)
      coupon ← * <~ Coupons
                .filterByContextAndFormId(ctx.id, couponCode.couponFormId)
                .filter(_.archivedAt.isEmpty)
                .mustFindOneOr(CouponWithCodeCannotBeFound(code))
      couponForm   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      couponShadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
      couponObject = IlluminatedCoupon.illuminate(ctx, coupon, couponForm, couponShadow)
      _ ← * <~ couponObject.mustBeActive
      _ ← * <~ couponObject.mustBeApplicable(couponCode, cart.accountId)
      // Fetch promotion + validate
      promotion ← * <~ Promotions
                   .filterByContextAndFormId(ctx.id, coupon.promotionId)
                   .filter(_.archivedAt.isEmpty)
                   .couponOnly
                   .mustFindOneOr(PromotionNotFoundForContext(coupon.promotionId, ctx.name))
      _ ← * <~ filterPromotionsUsingCustomerGroups(originator)(List(promotion))
           .ensure(OrderHasNoPromotions.single)(_.nonEmpty)
      promoForm   ← * <~ ObjectForms.mustFindById404(promotion.formId)
      promoShadow ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
      promoObject = IlluminatedPromotion.illuminate(ctx, promotion, promoForm, promoShadow)
      _ ← * <~ promoObject.mustBeActive

      // TODO: hmmmm, why is this needed? Shouldn’t be… @michalrus
      _ ← * <~ OrderPromotions.filterByCordRef(cart.refNum).deleteAll

      // Create connected promotion and line item adjustments
      _ ← * <~ OrderPromotions.create(OrderPromotion.buildCoupon(cart, promotion, couponCode))
      _ ← * <~ readjust(cart, failFatally = true)
      // Write event to application logs
      _ ← * <~ LogActivity().orderCouponAttached(cart, couponCode)
      // Response
      cart      ← * <~ CartTotaler.saveTotals(cart)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.buildRefreshed(cart)
    } yield TheResponse.validated(response, validated)

  def detachCoupon(originator: User, refNum: Option[String] = None)(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      ac: AC,
      ctx: OC): DbResultT[TheResponse[CartResponse]] =
    for {
      // Read
      cart            ← * <~ getCartByOriginator(originator, refNum)
      orderPromotions ← * <~ OrderPromotions.filterByCordRef(cart.refNum).couponOnly.result
      shadowIds = orderPromotions.map(_.promotionShadowId)
      promotions ← * <~ Promotions.filter(_.shadowId.inSet(shadowIds)).couponOnly.result
      deleteShadowIds = promotions.map(_.shadowId)
      // Write
      _ ← * <~ OrderPromotions.filterByOrderRefAndShadows(cart.refNum, deleteShadowIds).delete
      _ ← * <~ CartLineItemAdjustments
           .filterByOrderRefAndShadows(cart.refNum, deleteShadowIds)
           .delete
      _         ← * <~ CartTotaler.saveTotals(cart)
      _         ← * <~ LogActivity().orderCouponDetached(cart)
      validated ← * <~ CartValidator(cart).validate()
      response  ← * <~ CartResponse.buildRefreshed(cart)
    } yield TheResponse.validated(response, validated)

  /**
    * Getting only first discount now
    */
  private def tryDiscount[T](discounts: Seq[T]): Either[Failures, T] = discounts.headOption match {
    case Some(discount) ⇒ Either.right(discount)
    case _              ⇒ Either.left(EmptyDiscountFailure.single)
  }

  private def getAdjustments(promo: ObjectShadow, cart: Cart, qualifier: Qualifier, offer: Offer)(
      implicit ec: EC,
      apis: Apis,
      db: DB,
      au: AU): DbResultT[Seq[CartLineItemAdjustment]] =
    for {
      lineItems      ← * <~ LineItemManager.getCartLineItems(cart.refNum)
      shippingMethod ← * <~ shipping.ShippingMethods.forCordRef(cart.refNum).one
      subTotal       ← * <~ CartTotaler.subTotal(cart)
      shipTotal      ← * <~ CartTotaler.shippingTotal(cart)
      cartWithTotalsUpdated = cart.copy(subTotal = subTotal, shippingTotal = shipTotal)
      dqLineItems = lineItems.map { li ⇒
        DqLineItem(
          skuCode = li.sku.code,
          productId = li.productForm.id,
          price = li.price,
          lineItemType = if (li.isGiftCard) DqGiftCardLineItem else DqRegularLineItem,
          lineItemReferenceNumber = li.lineItemReferenceNumber
        )
      }
      input = DiscountInput(
        promotionShadowId = promo.id,
        cartRefNum = cart.referenceNumber,
        customerAccountId = cart.accountId,
        lineItems = dqLineItems,
        shippingCost = shippingMethod.map(_.price)
      )
      _            ← * <~ qualifier.check(input)
      offerResults ← * <~ offer.adjust(input)
    } yield offerResults.map(CartLineItemAdjustment.fromOfferResult)
}
