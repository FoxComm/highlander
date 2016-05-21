package services.coupon

import services.Result

import models.objects._
import models.coupon._
import models.promotion._

import responses.CouponResponses.{IlluminatedCouponResponse ⇒ Illuminated, _}
import utils.db._
import utils.db.DbResultT._
import slick.driver.PostgresDriver.api._
import payloads.{CreateCoupon, UpdateCoupon, GenerateCouponCodes}
import utils.aliases._
import failures.CouponFailures._
import failures.PromotionFailures._
import failures.ObjectFailures._

object CouponManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): Result[CouponFormResponse.Root] = (for {
    // guard to make sure the form is a coupon
    coupons ← * <~ Coupons.filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
    form    ← * <~ ObjectForms.mustFindById404(id)
  } yield CouponFormResponse.build(form)).run()

  def getShadow(id: Int, contextName: String)(implicit ec: EC, db: DB): Result[CouponShadowResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.mustFindOr(ObjectContextNotFound(contextName))
    coupon  ← * <~ Coupons.filterByContextAndFormId(context.id, id).one
                          .mustFindOr(CouponNotFoundForContext(id, contextName))
    shadow  ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
  } yield CouponShadowResponse.build(shadow)).run()

  def get(id: Int, contextName: String)(implicit ec: EC, db: DB): Result[CouponResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.mustFindOr(ObjectContextNotFound(contextName))
    coupon  ← * <~ Coupons.filterByContextAndFormId(context.id, id).one
                          .mustFindOr(CouponNotFoundForContext(id, contextName))
    form    ← * <~ ObjectForms.mustFindById404(coupon.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
  } yield CouponResponse.build(coupon, form, shadow)).run()

  def create(payload: CreateCoupon, contextName: String)(implicit ec: EC, db: DB): Result[CouponResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.mustFindOr(ObjectContextNotFound(contextName))
    _       ← * <~ Promotions.filterByContextAndFormId(context.id, payload.promotion).one
                             .mustFindOr(PromotionNotFoundForContext(payload.promotion, context.name))
    form    ← * <~ ObjectForm(kind = Coupon.kind, attributes = payload.form.attributes)
    shadow  ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
    ins     ← * <~ ObjectUtils.insert(form, shadow)
    coupon  ← * <~ Coupons.create(Coupon(contextId = context.id, formId = ins.form.id, shadowId = ins.shadow.id,
                                         commitId = ins.commit.id, promotionId = payload.promotion))
  } yield CouponResponse.build(coupon, ins.form, ins.shadow)).runTxn()

  def update(id: Int, payload: UpdateCoupon, contextName: String)
    (implicit ec: EC, db: DB): Result[CouponResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.mustFindOr(ObjectContextNotFound(contextName))
    _       ← * <~ Promotions.filterByContextAndFormId(context.id, payload.promotion).one
                             .mustFindOr(PromotionNotFoundForContext(payload.promotion, context.name))
    coupon  ← * <~ Coupons.filterByContextAndFormId(context.id, id).one
                          .mustFindOr(CouponNotFoundForContext(id, contextName))
    updated ← * <~ ObjectUtils.update(coupon.formId, coupon.shadowId, payload.form.attributes, payload.shadow.attributes)
    commit  ← * <~ ObjectUtils.commit(updated)
    coupon  ← * <~ updateHead(coupon, payload.promotion, updated.shadow, commit)
  } yield CouponResponse.build(coupon, updated.form, updated.shadow)).runTxn()

  def getIlluminated(id: Int, contextName: String)(implicit ec: EC, db: DB): Result[Illuminated.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.mustFindOr(ObjectContextNotFound(contextName))
    result  ← * <~ getIlluminatedIntern(id, context)
  } yield result).run()

  def getIlluminatedByCode(code: String, contextName: String)(implicit ec: EC, db: DB): Result[Illuminated.Root] = (for {
    context    ← * <~ ObjectContexts.filterByName(contextName).one.mustFindOr(ObjectContextNotFound(contextName))
    couponCode ← * <~ CouponCodes.filter(_.code.toLowerCase === code.toLowerCase).one
                                 .mustFindOr(CouponWithCodeCannotBeFound(code))
    result     ← * <~ getIlluminatedIntern(couponCode.couponFormId, context)
  } yield result).run()

  def getIlluminatedIntern(id: Int, context: ObjectContext)(implicit ec: EC, db: DB): DbResultT[Illuminated.Root] = for {
    coupon ← * <~ Coupons.filter(_.contextId === context.id).filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
    form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
    shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
  } yield Illuminated.build(IlluminatedCoupon.illuminate(context, coupon, form, shadow))

  def generateCode(id: Int, code: String)(implicit ec: EC, db: DB): Result[String] = (for {
    coupon     ← * <~ Coupons.filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
    couponCode ← * <~ CouponCodes.create(CouponCode(couponFormId = id, code = code))
  } yield couponCode.code).runTxn()

  def generateCodes(id: Int, payload: GenerateCouponCodes)(implicit ec: EC, db: DB): Result[Seq[String]] = (for {
    _         ← * <~ validateCouponCodePayload(payload)
    coupon    ← * <~ Coupons.filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
    generated ← * <~ CouponCodes.generateCodes(payload.prefix, payload.length, payload.quantity)
    unsaved   = generated.map { c ⇒ CouponCode(couponFormId = id, code = c) }
    _         ← * <~ CouponCodes.createAll(unsaved)
  } yield generated).runTxn()

  def getCodes(id: Int)(implicit ec: EC, db: DB): Result[Seq[CouponCodesResponse.Root]] = (for {
    _     ← * <~ Coupons.filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
    codes ← * <~ CouponCodes.filter(_.couponFormId === id).result
  } yield CouponCodesResponse.build(codes)).run()

  private def validateCouponCodePayload(p: GenerateCouponCodes)(implicit ec: EC) = {
    ObjectUtils.failIfErrors(
      Seq(
        if (p.quantity <= 0) Seq(CouponCodeQuanityMustBeGreaterThanZero) else Seq.empty,
        if (p.prefix.isEmpty) Seq(CouponCodePrefixNotSet) else Seq.empty,
        if (CouponCodes.isCharacterLimitValid(p.prefix.length, p.quantity, p.length)) Seq.empty
        else Seq(CouponCodeLengthIsTooSmall(p.prefix, p.quantity))
      ).flatten)
  }

  private def updateHead(coupon: Coupon, promotionId: Int, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])
    (implicit ec: EC): DbResult[Coupon] = maybeCommit match {
    case Some(commit) ⇒
      Coupons.update(coupon, coupon.copy(shadowId = shadow.id, commitId = commit.id, promotionId = promotionId))
    case None ⇒
      if (promotionId != coupon.promotionId) Coupons.update(coupon, coupon.copy(promotionId = promotionId))
      else DbResult.good(coupon)
  }
}
