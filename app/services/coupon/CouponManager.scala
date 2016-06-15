package services.coupon

import services.{LogActivity, Result}
import models.objects._
import models.coupon._
import models.promotion._
import responses.CouponResponses.{IlluminatedCouponResponse ⇒ Illuminated, _}
import utils.db._
import utils.db.DbResultT._
import slick.driver.PostgresDriver.api._
import payloads.CouponPayloads._
import utils.aliases._
import failures.CouponFailures._
import failures.PromotionFailures._
import failures.ObjectFailures._
import models.StoreAdmin

object CouponManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): Result[CouponFormResponse.Root] =
    (for {
      // guard to make sure the form is a coupon
      coupons ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      form    ← * <~ ObjectForms.mustFindById404(id)
    } yield CouponFormResponse.build(form)).run()

  def getShadow(
      id: Int, contextName: String)(implicit ec: EC, db: DB): Result[CouponShadowResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, id)
                .mustFindOneOr(CouponNotFoundForContext(id, contextName))
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield CouponShadowResponse.build(shadow)).run()

  def get(id: Int, contextName: String)(implicit ec: EC, db: DB): Result[CouponResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, id)
                .mustFindOneOr(CouponNotFoundForContext(id, contextName))
      form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield CouponResponse.build(coupon, form, shadow)).run()

  def create(payload: CreateCoupon, contextName: String, admin: Option[StoreAdmin])(
      implicit ec: EC, db: DB, ac: AC): Result[CouponResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      _ ← * <~ Promotions
           .filterByContextAndFormId(context.id, payload.promotion)
           .mustFindOneOr(PromotionNotFoundForContext(payload.promotion, context.name))
      form   ← * <~ ObjectForm(kind = Coupon.kind, attributes = payload.form.attributes)
      shadow ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
      ins    ← * <~ ObjectUtils.insert(form, shadow)
      coupon ← * <~ Coupons.create(
                  Coupon(contextId = context.id,
                         formId = ins.form.id,
                         shadowId = ins.shadow.id,
                         commitId = ins.commit.id,
                         promotionId = payload.promotion))
      response = CouponResponse.build(coupon, ins.form, ins.shadow)
      _ ← * <~ LogActivity.couponCreated(response, admin)
    } yield response).runTxn()

  def update(id: Int, payload: UpdateCoupon, contextName: String, admin: StoreAdmin)(
      implicit ec: EC, db: DB, ac: AC): Result[CouponResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      _ ← * <~ Promotions
           .filterByContextAndFormId(context.id, payload.promotion)
           .mustFindOneOr(PromotionNotFoundForContext(payload.promotion, context.name))
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, id)
                .mustFindOneOr(CouponNotFoundForContext(id, contextName))
      updated ← * <~ ObjectUtils.update(coupon.formId,
                                        coupon.shadowId,
                                        payload.form.attributes,
                                        payload.shadow.attributes)
      commit ← * <~ ObjectUtils.commit(updated)
      coupon ← * <~ updateHead(coupon, payload.promotion, updated.shadow, commit)
      response = CouponResponse.build(coupon, updated.form, updated.shadow)
      _ ← * <~ LogActivity.couponUpdated(response, Some(admin))
    } yield response).runTxn()

  def getIlluminated(
      id: Int, contextName: String)(implicit ec: EC, db: DB): Result[Illuminated.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      result ← * <~ getIlluminatedIntern(id, context)
    } yield result).run()

  def getIlluminatedByCode(
      code: String, contextName: String)(implicit ec: EC, db: DB): Result[Illuminated.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      couponCode ← * <~ CouponCodes
                    .filter(_.code.toLowerCase === code.toLowerCase)
                    .mustFindOneOr(CouponWithCodeCannotBeFound(code))
      result ← * <~ getIlluminatedIntern(couponCode.couponFormId, context)
    } yield result).run()

  def getIlluminatedIntern(id: Int, context: ObjectContext)(
      implicit ec: EC, db: DB): DbResultT[Illuminated.Root] =
    for {
      coupon ← * <~ Coupons
                .filter(_.contextId === context.id)
                .filter(_.formId === id)
                .mustFindOneOr(CouponNotFound(id))
      form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield Illuminated.build(IlluminatedCoupon.illuminate(context, coupon, form, shadow))

  def generateCode(id: Int, code: String, admin: StoreAdmin)(
      implicit ec: EC, db: DB, ac: AC): Result[String] =
    (for {
      coupon     ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      couponCode ← * <~ CouponCodes.create(CouponCode(couponFormId = id, code = code))
      _          ← * <~ LogActivity.singleCouponCodeCreated(coupon, Some(admin))
    } yield couponCode.code).runTxn()

  def generateCodes(id: Int, payload: GenerateCouponCodes, admin: StoreAdmin)(
      implicit ec: EC, db: DB, ac: AC): Result[Seq[String]] =
    (for {
      _         ← * <~ validateCouponCodePayload(payload)
      coupon    ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      generated ← * <~ CouponCodes.generateCodes(payload.prefix, payload.length, payload.quantity)
      unsaved = generated.map { c ⇒
        CouponCode(couponFormId = id, code = c)
      }
      _ ← * <~ CouponCodes.createAll(unsaved)
      _ ← * <~ LogActivity.multipleCouponCodeCreated(coupon, Some(admin))
    } yield generated).runTxn()

  def getCodes(id: Int)(implicit ec: EC, db: DB): Result[Seq[CouponCodesResponse.Root]] =
    (for {
      _     ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
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

  private def updateHead(
      coupon: Coupon, promotionId: Int, shadow: ObjectShadow, maybeCommit: Option[ObjectCommit])(
      implicit ec: EC): DbResult[Coupon] = maybeCommit match {
    case Some(commit) ⇒
      // TODO @anna: #longlivedbresultt
      Coupons
        .update(coupon,
                coupon.copy(shadowId = shadow.id, commitId = commit.id, promotionId = promotionId))
        .value
    case None ⇒
      // TODO @anna: #longlivedbresultt
      if (promotionId != coupon.promotionId)
        Coupons.update(coupon, coupon.copy(promotionId = promotionId)).value
      else DbResult.good(coupon)
  }
}
