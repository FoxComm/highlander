package services.promotion

import failures.ObjectFailures._
import failures.PromotionFailures._
import models.discount._
import models.objects._
import models.promotion._
import payloads.DiscountPayloads._
import payloads.PromotionPayloads._
import responses.PromotionResponses._
import services.Result
import services.discount.DiscountManager
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db.DbResultT._
import utils.db._

object PromotionManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): Result[PromotionFormResponse.Root] = (for {
    //guard to make sure the form is a promotion
    _    ← * <~ Promotions.filter(_.formId === id).mustFindOneOr(PromotionNotFound(id))
    form ← * <~ ObjectForms.mustFindById404(id)
  } yield PromotionFormResponse.build(form)).run()

  def getShadow(id: Int, contextName: String)(implicit ec: EC, db: DB): Result[PromotionShadowResponse.Root] = (for {
    context         ← * <~ ObjectContexts.filterByName(contextName).mustFindOneOr(ObjectContextNotFound(contextName))
    promotion       ← * <~ Promotions.filterByContextAndFormId(context.id, id)
                                     .mustFindOneOr(PromotionNotFoundForContext(id, contextName))
    shadow          ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
    discountLinks   ← * <~ ObjectLinks.findByLeftAndType(shadow.id, ObjectLink.PromotionDiscount).result
    discountShadowIds = discountLinks.map(_.rightId)
    discountShadows ← * <~ ObjectShadows.filter(_.id.inSet(discountShadowIds)).result
  } yield PromotionShadowResponse.build(shadow, discountShadows)).run()

  def get(id: Int, contextName: String)(implicit ec: EC, db: DB): Result[PromotionResponse.Root] = (for {
    context         ← * <~ ObjectContexts.filterByName(contextName).mustFindOneOr(ObjectContextNotFound(contextName))
    promotion       ← * <~ Promotions.filterByContextAndFormId(context.id, id)
                                     .mustFindOneOr(PromotionNotFoundForContext(id, contextName))
    form            ← * <~ ObjectForms.mustFindById404(promotion.formId)
    shadow          ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
    discounts       ← * <~ ObjectUtils.getChildren(promotion.shadowId, ObjectLink.PromotionDiscount)
    discountForms   = discounts.map(_.form)
    discountShadows = discounts.map(_.shadow)
  } yield PromotionResponse.build(promotion, form, shadow, discountForms, discountShadows)).run()

  def create(payload: CreatePromotion, contextName: String)
    (implicit ec: EC, db: DB): Result[PromotionResponse.Root] = (for {
    context   ← * <~ ObjectContexts.filterByName(contextName).mustFindOneOr(ObjectContextNotFound(contextName))
    form      ← * <~ ObjectForm(kind = Promotion.kind, attributes = payload.form.attributes)
    shadow    ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
    ins       ← * <~ ObjectUtils.insert(form, shadow)
    promotion ← * <~ Promotions.create(Promotion(contextId = context.id, applyType = payload.applyType,
                                       formId = ins.form.id, shadowId = ins.shadow.id, commitId = ins.commit.id))
    discount  ← * <~ createDiscounts(context, payload, ins.shadow)
  } yield PromotionResponse.build(promotion, ins.form, ins.shadow, discount.forms, discount.shadows)).runTxn()

  private case class DiscountsCreateResult(forms: Seq[ObjectForm], shadows: Seq[ObjectShadow])

  private def createDiscounts(context: ObjectContext, payload: CreatePromotion, promotionShadow: ObjectShadow)
    (implicit ec: EC): DbResultT[DiscountsCreateResult] = for {
    discounts    ← * <~ payload.form.discounts.zip(payload.shadow.discounts)
    newDiscounts ← * <~ DbResultT.sequence(discounts.map { case (form, shadow) ⇒
                          createDiscount(context, form, shadow, promotionShadow)
                        })
    forms        = newDiscounts.map(_.form)
    shadows      = newDiscounts.map(_.shadow)
  } yield DiscountsCreateResult(forms, shadows)

  private case class DiscountCreateResult(form: ObjectForm, shadow: ObjectShadow)

  private def createDiscount(context: ObjectContext, formPayload: CreateDiscountForm,
    shadowPayload: CreateDiscountShadow, promotionShadow: ObjectShadow)(implicit ec: EC): DbResultT[DiscountCreateResult] = for {
    discount ← * <~ DiscountManager.createInternal(CreateDiscount(form = formPayload, shadow = shadowPayload), context)
    link     ← * <~ ObjectLinks.create(ObjectLink(leftId = promotionShadow.id, rightId = discount.shadow.id,
                                                  linkType = ObjectLink.PromotionDiscount))
  } yield DiscountCreateResult(discount.form, discount.shadow)

  def update(id: Int, payload: UpdatePromotion, contextName: String)
    (implicit ec: EC, db: DB): Result[PromotionResponse.Root] = (for {
    context   ← * <~ ObjectContexts.filterByName(contextName).mustFindOneOr(ObjectContextNotFound(contextName))
    promotion ← * <~ Promotions.filterByContextAndFormId(context.id, id)
                               .mustFindOneOr(PromotionNotFoundForContext(id, contextName))
    updated   ← * <~ ObjectUtils.update(promotion.formId, promotion.shadowId, payload.form.attributes,
                                        payload.shadow.attributes)
    discount  ← * <~ updateDiscounts(context, promotion.shadowId, updated.shadow.id, payload)
    commit    ← * <~ ObjectUtils.commit(updated)
    promotion ← * <~ updateHead(promotion, payload, updated.shadow, commit)
  } yield PromotionResponse.build(promotion, updated.form, updated.shadow, discount.forms, discount
    .shadows)).runTxn()

  private case class UpdateDiscountsResult(forms: Seq[ObjectForm], shadows: Seq[ObjectShadow])

  private def updateDiscounts(context: ObjectContext, oldPromotionShadowId: Int, promotionShadowId: Int,
    payload: UpdatePromotion)(implicit ec: EC, db: DB): DbResultT[UpdateDiscountsResult] = {

    val pairs = payload.form.discounts.sortBy(_.id).zip(payload.shadow.discounts.sortBy(_.id))

    for {
      updated ← * <~ DbResultT.sequence(pairs.map { case (form, shadow) ⇒
                       updateDiscount(context, form, shadow, oldPromotionShadowId, promotionShadowId)
                     })
      forms   = updated.map(_.form)
      shadows = updated.map(_.shadow)
    } yield UpdateDiscountsResult(forms, shadows)
  }

  private case class UpdateDiscountResult(form: ObjectForm, shadow: ObjectShadow)

  private def updateDiscount(context: ObjectContext, formPayload: UpdatePromoDiscountForm,
    shadowPayload: UpdatePromoDiscountShadow, oldPromotionShadowId: Int, promotionShadowId: Int)
    (implicit ec: EC, db: DB): DbResultT[UpdateDiscountResult] = {
    val updateDiscountPayload = UpdateDiscount(
      form = UpdateDiscountForm(attributes = formPayload.attributes),
      shadow = UpdateDiscountShadow(attributes = shadowPayload.attributes))

    for {
      discount ← * <~ DiscountManager.updateInternal(formPayload.id, updateDiscountPayload, context)
      _        ← * <~ ObjectUtils.updateLink(oldPromotionShadowId, promotionShadowId, discount.oldDiscount.shadowId,
                                             discount.shadow.id, linkType = ObjectLink.PromotionDiscount)
    } yield UpdateDiscountResult(discount.form, discount.shadow)
  }

  def getIlluminated(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedPromotionResponse.Root] = (for {
    context   ← * <~ ObjectContexts.filterByName(contextName).mustFindOneOr(ObjectContextNotFound(contextName))
    promotion ← * <~ Promotions.filter(_.contextId === context.id).filter(_.formId === id)
                               .mustFindOneOr(PromotionNotFound(id))
    form      ← * <~ ObjectForms.mustFindById404(promotion.formId)
    shadow    ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
    discounts ← * <~ ObjectUtils.getChildren(promotion.shadowId, ObjectLink.PromotionDiscount)
  } yield IlluminatedPromotionResponse.build(
    promotion = IlluminatedPromotion.illuminate(context, promotion, form, shadow),
    discounts = discounts.map(d ⇒ IlluminatedDiscount.illuminate(form = d.form, shadow = d.shadow)))).run()

  private def updateHead(promotion: Promotion, payload: UpdatePromotion, shadow: ObjectShadow,
    maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResult[Promotion] = maybeCommit match {
    case Some(commit) ⇒
      val updated = promotion.copy(applyType = payload.applyType, shadowId = shadow.id, commitId = commit.id)
      Promotions.update(promotion, updated)
    case None ⇒
      if (promotion.applyType != payload.applyType)
        Promotions.update(promotion, promotion.copy(applyType = payload.applyType))
      else DbResult.good(promotion)
  }
}
