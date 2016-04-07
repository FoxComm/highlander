package services.promotion

import services.Result
import services.discount.DiscountManager

import models.objects._
import models.promotion._
import models.discount._

import responses.PromotionResponses._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._
import payloads.{CreatePromotion, CreatePromotionForm, CreatePromotionShadow, 
  UpdatePromotion, UpdatePromotionForm, UpdatePromotionShadow, CreateDiscount,
  UpdateDiscount, CreateDiscountForm, CreateDiscountShadow, UpdatePromoDiscountForm, 
  UpdatePromoDiscountShadow, UpdateDiscountForm, UpdateDiscountShadow}
import utils.aliases._
import cats.data.NonEmptyList
import failures.NotFoundFailure404
import failures.PromotionFailures._
import failures.ObjectFailures._

object PromotionManager {

  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[PromotionFormResponse.Root] = (for {
    //guard to make sure the form is a promotion
    promotions ← * <~ Promotions.filter(_.formId === id).one.mustFindOr(PromotionNotFound(id))
    form  ← * <~ ObjectForms.mustFindById404(id)
  } yield PromotionFormResponse.build(form)).run()

  def getShadow(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[PromotionShadowResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    promotion     ← * <~ Promotions.filterByContextAndFormId(context.id, id).one.
      mustFindOr(PromotionNotFoundForContext(id, contextName))
    shadow  ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
    discountLinks ← * <~ ObjectLinks.filter(_.leftId === shadow.id).result
    discountShadowIds = discountLinks.map(_.rightId) 
    discountShadows ← * <~ ObjectShadows.filter(_.id.inSet(discountShadowIds)).result
  } yield PromotionShadowResponse.build(shadow, discountShadows)).run()

  def get(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[PromotionResponse.Root] = (for {
      context  ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
      promotion     ← * <~ Promotions.filterByContextAndFormId(context.id, id).one.
        mustFindOr(PromotionNotFoundForContext(id, contextName))
      form     ← * <~ ObjectForms.mustFindById404(promotion.formId)
      shadow   ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
      discounts ← * <~ ObjectUtils.getChildren(promotion.shadowId)
      discountForms ← * <~ discounts.map(_.form)
      discountShadows ← * <~ discounts.map(_.shadow)
  } yield PromotionResponse.build(form, shadow, discountForms, discountShadows)).run()

  def create(payload: CreatePromotion, contextName: String) 
    (implicit ec: EC, db: DB): Result[PromotionResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    form    ← * <~ ObjectForm(kind = Promotion.kind, attributes = 
      payload.form.attributes)
    shadow  ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
    ins     ← * <~ ObjectUtils.insert(form, shadow)
    promotion ← * <~ Promotions.create(Promotion(contextId = context.id, 
      formId = ins.form.id, shadowId = ins.shadow.id, commitId = ins.commit.id))
    discount ← * <~ createDiscounts(context, payload, ins.shadow)
  } yield PromotionResponse.build(ins.form, ins.shadow, discount.forms, discount.shadows)).runTxn()

  private final case class DiscountsCreateResult(forms: Seq[ObjectForm], shadows: Seq[ObjectShadow])
  private def createDiscounts(context: ObjectContext, payload: CreatePromotion, promotionShadow: ObjectShadow)
    (implicit ec: EC, db: DB): DbResultT[DiscountsCreateResult] = for {
      ps ← * <~ payload.form.discounts.zip(payload.shadow.discounts)
      rs     ← * <~ DbResultT.sequence( 
        ps.map{ case (form, shadow) ⇒ 
            createDiscount(context, form, shadow, promotionShadow)
        })
      forms   ← * <~ rs.map(_.form)
      shadows ← * <~ rs.map(_.shadow)
    } yield DiscountsCreateResult(forms, shadows)

  private final case class DiscountCreateResult(form: ObjectForm, shadow: ObjectShadow)

  private def createDiscount(context: ObjectContext, formPayload: CreateDiscountForm, 
    shadowPayload: CreateDiscountShadow, promotionShadow: ObjectShadow)
  (implicit ec: EC, db: DB): DbResultT[DiscountCreateResult] = {
    val payload = CreateDiscount(form = formPayload, shadow = shadowPayload)
    for {
      r ← * <~ DiscountManager.createInternal(payload, context)
      link ← * <~ ObjectLinks.create(ObjectLink(leftId = promotionShadow.id, rightId = r.shadow.id))
    } yield DiscountCreateResult(r.form, r.shadow)
  }

  def update(id: Int, payload: UpdatePromotion, contextName: String)
    (implicit ec: EC, db: DB): Result[PromotionResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    promotion     ← * <~ Promotions.filterByContextAndFormId(context.id, id).one.
      mustFindOr(PromotionNotFoundForContext(id, contextName))
    updatedPromotion ← * <~ ObjectUtils.update(promotion.formId, promotion.shadowId, 
      payload.form.attributes, payload.shadow.attributes)
    discount ← * <~ updateDiscounts(context, promotion.shadowId, 
      updatedPromotion.shadow.id, payload)
    commit ← * <~ ObjectUtils.commit(updatedPromotion)
    promotion ← * <~ updateHead(promotion, updatedPromotion.shadow, commit)
  } yield PromotionResponse.build(updatedPromotion.form, updatedPromotion.shadow,
    discount.forms, discount.shadows)).runTxn()

  private final case class UpdateDiscountsResult(forms: Seq[ObjectForm], shadows: Seq[ObjectShadow])

  private def updateDiscounts(context: ObjectContext, oldPromotionShadowId: Int, 
    promotionShadowId: Int, payload: UpdatePromotion)
    (implicit ec: EC, db: DB): DbResultT[UpdateDiscountsResult] = { 

      val pairs =  payload.form.discounts.sortBy(_.id).zip(payload.shadow.discounts.sortBy(_.id))

      for {
        rs ← * <~ DbResultT.sequence(
          pairs.map{ case (form, shadow) ⇒  
            updateDiscount(context, form, shadow, oldPromotionShadowId, 
              promotionShadowId)
          })
        forms ← * <~ rs.map(_.form)
        shadows ← * <~ rs.map(_.shadow)
      } yield UpdateDiscountsResult(forms, shadows)
    }

  private final case class UpdateDiscountResult(form: ObjectForm, shadow: ObjectShadow)

  private def updateDiscount(context: ObjectContext, formPayload: UpdatePromoDiscountForm, 
    shadowPayload: UpdatePromoDiscountShadow, oldPromotionShadowId: Int, 
    promotionShadowId: Int)
    (implicit ec: EC, db: DB): DbResultT[UpdateDiscountResult] = {
      val updateDiscountPayload = UpdateDiscount(
        form = UpdateDiscountForm(attributes = formPayload.attributes),
        shadow = UpdateDiscountShadow(attributes = shadowPayload.attributes))

      for {
        discount ← * <~ DiscountManager.updateInternal(formPayload.id, 
          updateDiscountPayload, context) 
        _ ← * <~ ObjectUtils.updateLink(oldPromotionShadowId, promotionShadowId, 
          discount.oldDiscount.shadowId, discount.shadow.id)
      } yield UpdateDiscountResult(discount.form, discount.shadow)
    }

  def getIlluminated(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedPromotionResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    promotion     ← * <~ Promotions.filter(_.contextId === context.id).
      filter(_.formId === id).one.mustFindOr(PromotionNotFound(id))
    form    ← * <~ ObjectForms.mustFindById404(promotion.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(promotion.shadowId)
    discounts ← * <~ ObjectUtils.getChildren(promotion.shadowId)
  } yield IlluminatedPromotionResponse.build(
    promotion = IlluminatedPromotion.illuminate(context, promotion, form, shadow),
    discounts = discounts.map(d ⇒ IlluminatedDiscount.illuminate(form = d.form, 
      shadow = d.shadow)
      ))).run()

  private def updateHead(promotion: Promotion, shadow: ObjectShadow, 
    maybeCommit: Option[ObjectCommit]) 
    (implicit ec: EC, db: DB): DbResultT[Product] = 
      maybeCommit match {
        case Some(commit) ⇒  for { 
          promotion   ← * <~ Promotions.update(promotion, promotion.copy(
            shadowId = shadow.id, commitId = commit.id))
        } yield promotion
        case None ⇒ DbResultT.pure(promotion)
      }
}
