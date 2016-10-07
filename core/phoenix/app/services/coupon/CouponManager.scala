package services.coupon

import java.time.Instant

import com.github.tminglei.slickpg.LTree
import failures.CouponFailures._
import failures.NotFoundFailure404
import failures.ObjectFailures._
import failures.PromotionFailures._
import models.account._
import models.coupon._
import models.objects._
import models.promotion._
import payloads.CouponPayloads._
import responses.CouponResponses.{IlluminatedCouponResponse ⇒ Illuminated, _}
import services.LogActivity
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CouponManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): DbResultT[CouponFormResponse.Root] =
    for {
      // guard to make sure the form is a coupon
      coupons ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      form    ← * <~ ObjectForms.mustFindById404(id)
    } yield CouponFormResponse.build(form)

  def getShadow(id: Int, contextName: String)(implicit ec: EC,
                                              db: DB): DbResultT[CouponShadowResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, id)
                .mustFindOneOr(CouponNotFoundForContext(id, contextName))
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield CouponShadowResponse.build(shadow)

  def get(id: Int, contextName: String)(implicit ec: EC, db: DB): DbResultT[CouponResponse.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      coupon ← * <~ Coupons
                .filterByContextAndFormId(context.id, id)
                .mustFindOneOr(CouponNotFoundForContext(id, contextName))
      form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield CouponResponse.build(coupon, form, shadow))

  def create(payload: CreateCoupon, contextName: String, admin: Option[User])(
      implicit ec: EC,
      db: DB,
      ac: AC,
      au: AU): DbResultT[CouponResponse.Root] =
    for {

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
                  Coupon(scope = LTree(au.token.scope),
                         contextId = context.id,
                         formId = ins.form.id,
                         shadowId = ins.shadow.id,
                         commitId = ins.commit.id,
                         promotionId = payload.promotion))
      response = CouponResponse.build(coupon, ins.form, ins.shadow)
      _ ← * <~ LogActivity.couponCreated(response, admin)
    } yield response

  def update(id: Int, payload: UpdateCoupon, contextName: String, admin: User)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[CouponResponse.Root] =
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
    } yield response)

  def getIlluminated(id: Int, contextName: String)(implicit ec: EC,
                                                   db: DB): DbResultT[Illuminated.Root] =
    (for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      result ← * <~ getIlluminatedIntern(id, context)
    } yield result)

  def getIlluminatedByCode(code: String, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[Illuminated.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      couponCode ← * <~ CouponCodes
                    .filter(_.code.toLowerCase === code.toLowerCase)
                    .mustFindOneOr(CouponWithCodeCannotBeFound(code))
      result ← * <~ getIlluminatedIntern(couponCode.couponFormId, context)
    } yield result

  def getIlluminatedIntern(id: Int, context: ObjectContext)(implicit ec: EC,
                                                            db: DB): DbResultT[Illuminated.Root] =
    for {
      coupon ← * <~ Coupons
                .filter(_.contextId === context.id)
                .filter(_.formId === id)
                .mustFindOneOr(CouponNotFound(id))
      form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield Illuminated.build(IlluminatedCoupon.illuminate(context, coupon, form, shadow))

  def archiveByContextAndId(contextName: String,
                            formId: Int)(implicit ec: EC, db: DB): DbResultT[CouponResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      model ← * <~ Coupons
               .findOneByContextAndFormId(context.id, formId)
               .mustFindOneOr(NotFoundFailure404(Coupon, formId))
      archiveResult ← * <~ Coupons.update(model, model.copy(archivedAt = Some(Instant.now)))
      form          ← * <~ ObjectForms.mustFindById404(archiveResult.formId)
      shadow        ← * <~ ObjectShadows.mustFindById404(archiveResult.shadowId)
    } yield CouponResponse.build(archiveResult, form, shadow)

  def generateCode(id: Int, code: String, admin: User)(implicit ec: EC,
                                                       db: DB,
                                                       ac: AC): DbResultT[String] =
    for {
      coupon     ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      couponCode ← * <~ CouponCodes.create(CouponCode(couponFormId = id, code = code))
      _          ← * <~ LogActivity.singleCouponCodeCreated(coupon, Some(admin))
    } yield couponCode.code

  def generateCodes(id: Int,
                    payload: GenerateCouponCodes,
                    admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[Seq[String]] =
    for {
      _         ← * <~ validateCouponCodePayload(payload)
      coupon    ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      generated ← * <~ CouponCodes.generateCodes(payload.prefix, payload.length, payload.quantity)
      unsaved = generated.map { c ⇒
        CouponCode(couponFormId = id, code = c)
      }
      _ ← * <~ CouponCodes.createAll(unsaved)
      _ ← * <~ LogActivity.multipleCouponCodeCreated(coupon, Some(admin))
    } yield generated

  def getCodes(id: Int)(implicit ec: EC, db: DB): DbResultT[Seq[CouponCodesResponse.Root]] =
    for {
      _     ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      codes ← * <~ CouponCodes.filter(_.couponFormId === id).result
    } yield CouponCodesResponse.build(codes)

  private def validateCouponCodePayload(p: GenerateCouponCodes)(implicit ec: EC) = {
    ObjectUtils.failIfErrors(
        Seq(
            if (p.quantity <= 0) Seq(CouponCodeQuanityMustBeGreaterThanZero)
            else Seq.empty,
            if (p.prefix.isEmpty) Seq(CouponCodePrefixNotSet) else Seq.empty,
            if (CouponCodes.isCharacterLimitValid(p.prefix.length, p.quantity, p.length))
              Seq.empty
            else Seq(CouponCodeLengthIsTooSmall(p.prefix, p.quantity))
        ).flatten)
  }

  private def updateHead(coupon: Coupon,
                         promotionId: Int,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Coupon] =
    maybeCommit match {
      case Some(commit) ⇒
        Coupons.update(
            coupon,
            coupon.copy(shadowId = shadow.id, commitId = commit.id, promotionId = promotionId))
      case None ⇒
        if (promotionId != coupon.promotionId)
          Coupons.update(coupon, coupon.copy(promotionId = promotionId))
        else DbResultT.good(coupon)
    }
}
