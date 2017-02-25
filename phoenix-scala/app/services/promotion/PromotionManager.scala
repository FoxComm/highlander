package services.promotion

import java.time.Instant

import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.account._
import models.coupon.Coupons
import models.discount._
import models.objects.ObjectUtils._
import models.objects._
import models.promotion._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads._
import responses.PromotionResponses._
import services.LogActivity
import services.discount.DiscountManager
import services.objects.ObjectManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object PromotionManager {

  def create(payload: CreatePromotion, contextName: String, admin: Option[User])(
      implicit ec: EC,
      db: DB,
      ac: AC,
      au: AU): DbResultT[PromotionResponse.Root] = {
    val formAndShadow =
      FormAndShadow.fromPayload(kind = Promotion.kind, attributes = payload.attributes)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      (form, shadow) = IlluminatedPromotion
        .validatePromotion(payload.applyType, formAndShadow)
        .tupled
      ins ← * <~ ObjectUtils.insert((form, shadow), payload.schema)
      promotion ← * <~ Promotions.create(
                     Promotion(scope = scope,
                               contextId = context.id,
                               applyType = payload.applyType,
                               formId = ins.form.id,
                               shadowId = ins.shadow.id,
                               commitId = ins.commit.id))
      discount ← * <~ createDiscounts(context, payload, ins.shadow)
      response = PromotionResponse
        .build(context, promotion, ins.form, ins.shadow, discount.forms.zip(discount.shadows))
      _ ← * <~ LogActivity.promotionCreated(response, admin)
    } yield response
  }

  private case class DiscountsCreateResult(forms: Seq[ObjectForm], shadows: Seq[ObjectShadow])

  private def createDiscounts(
      context: ObjectContext,
      payload: CreatePromotion,
      promotionShadow: ObjectShadow)(implicit ec: EC, au: AU): DbResultT[DiscountsCreateResult] =
    for {
      newDiscounts ← * <~ payload.discounts.map { discount ⇒
                      createDiscount(context, discount, promotionShadow)
                    }
      forms   = newDiscounts.map(_.form)
      shadows = newDiscounts.map(_.shadow)
    } yield DiscountsCreateResult(forms, shadows)

  private case class DiscountCreateResult(form: ObjectForm, shadow: ObjectShadow)

  private def createDiscount(
      context: ObjectContext,
      createDiscount: CreateDiscount,
      promotionShadow: ObjectShadow)(implicit ec: EC, au: AU): DbResultT[DiscountCreateResult] =
    for {
      promotion ← * <~ Promotions
                   .filterByContextAndShadowId(context.id, promotionShadow.id)
                   .mustFindOneOr(NotFoundFailure404(classOf[Promotion].getSimpleName,
                                                     "shadowId",
                                                     promotionShadow.id))
      discount ← * <~ DiscountManager
                  .createInternal(CreateDiscount(attributes = createDiscount.attributes), context)
      link ← * <~ PromotionDiscountLinks.create(
                PromotionDiscountLink(leftId = promotion.id, rightId = discount.discount.id))
    } yield DiscountCreateResult(discount.form, discount.shadow)

  def update(id: Int, payload: UpdatePromotion, contextName: String, admin: Option[User])(
      implicit ec: EC,
      ac: AC,
      db: DB): DbResultT[PromotionResponse.Root] = {

    val formAndShadow = FormAndShadow.fromPayload(Promotion.kind, payload.attributes)

    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      promotion ← * <~ Promotions.filterByContextAndFormId(context.id, id) ~> Promotions
                   .notFound404(("context", context.name), ("id", id))
      validated = IlluminatedPromotion
        .validatePromotion(payload.applyType, (formAndShadow.form, formAndShadow.shadow))

      updated ← * <~ ObjectUtils.update(promotion.formId,
                                        promotion.shadowId,
                                        validated.form.attributes,
                                        validated.shadow.attributes)
      discount  ← * <~ updateDiscounts(context, payload)
      commit    ← * <~ ObjectUtils.commit(updated)
      promotion ← * <~ updateHead(promotion, payload, updated.shadow, commit)
      response = PromotionResponse.build(context,
                                         promotion,
                                         updated.form,
                                         updated.shadow,
                                         discount.forms.zip(discount.shadows))
      _ ← * <~ LogActivity.promotionUpdated(response, admin)
    } yield response
  }

  def archiveByContextAndId(contextName: String, formId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[PromotionResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      fullObject ← * <~ ObjectManager.getFullObject(
                      mustFindPromotionByContextAndFormId(context.id, formId))
      model = fullObject.model
      now   = Some(Instant.now)
      archiveResult ← * <~ Promotions.update(model, model.copy(archivedAt = now))
      coupons       ← * <~ Coupons.filterByContextAndPromotionId(context.id, archiveResult.id).result
      _             ← * <~ coupons.map(coupon ⇒ Coupons.update(coupon, coupon.copy(archivedAt = now)))
      discounts     ← * <~ PromotionDiscountLinks.queryRightByLeft(archiveResult)
      discountForms   = discounts.map(_.form)
      discountShadows = discounts.map(_.shadow)
    } yield
      PromotionResponse.build(context,
                              archiveResult,
                              fullObject.form,
                              fullObject.shadow,
                              discountForms.zip(discountShadows))

  private def mustFindPromotionByContextAndFormId(contextId: Int, formId: Int)(
      implicit ec: EC,
      db: DB): DbResultT[Promotion] =
    Promotions
      .findOneByContextAndFormId(contextId, formId)
      .mustFindOneOr(NotFoundFailure404(Promotion, formId))

  private case class UpdateDiscountsResult(forms: Seq[ObjectForm], shadows: Seq[ObjectShadow])

  private def updateDiscounts(context: ObjectContext, payload: UpdatePromotion)(
      implicit ec: EC,
      db: DB): DbResultT[UpdateDiscountsResult] = {

    for {
      updated ← * <~ payload.discounts.sortBy(_.id).map { discount ⇒
                 updateDiscount(context, discount)
               }
      forms   = updated.map(_.form)
      shadows = updated.map(_.shadow)
    } yield UpdateDiscountsResult(forms, shadows)
  }

  private case class UpdateDiscountResult(form: ObjectForm, shadow: ObjectShadow)

  private def updateDiscount(context: ObjectContext, updateDiscount: UpdatePromoDiscount)(
      implicit ec: EC,
      db: DB): DbResultT[UpdateDiscountResult] = {

    for {
      discount ← * <~ DiscountManager
                  .updateInternal(updateDiscount.id, updateDiscount.attributes, context)
    } yield UpdateDiscountResult(discount.form, discount.shadow)
  }

  def getIlluminated(id: Int, contextName: String)(implicit ec: EC,
                                                   db: DB): DbResultT[PromotionResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      promotion ← * <~ Promotions
                   .filter(_.contextId === context.id)
                   .filter(_.formId === id) ~> Promotions.notFound404(("context", context.name),
                                                                      ("id", id))
      form      ← * <~ ObjectForms.findById(promotion.formId)
      shadow    ← * <~ ObjectShadows.findById(promotion.shadowId)
      discounts ← * <~ PromotionDiscountLinks.queryRightByLeft(promotion)
    } yield
      PromotionResponse.build(
          promotion = IlluminatedPromotion.illuminate(context, promotion, form, shadow),
          discounts =
            discounts.map(d ⇒ IlluminatedDiscount.illuminate(form = d.form, shadow = d.shadow)),
          promotion)

  private def updateHead(
      promotion: Promotion,
      payload: UpdatePromotion,
      shadow: ObjectShadow,
      maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Promotion] =
    maybeCommit match {
      case Some(commit) ⇒
        val updated =
          promotion.copy(applyType = payload.applyType, shadowId = shadow.id, commitId = commit.id)
        Promotions.update(promotion, updated)
      case None ⇒
        if (promotion.applyType != payload.applyType)
          Promotions.update(promotion, promotion.copy(applyType = payload.applyType))
        else
          DbResultT.good(promotion)
    }
}
