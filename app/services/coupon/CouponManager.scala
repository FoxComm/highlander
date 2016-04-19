package services.coupon

import services.Result

import models.objects._
import models.coupon._
import models.promotion._

import responses.CouponResponses._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._
import payloads.{CreateCoupon, CreateCouponForm, CreateCouponShadow, 
  UpdateCoupon, UpdateCouponForm, UpdateCouponShadow, GenerateCouponCodes}
import utils.aliases._
import cats.data.NonEmptyList
import failures.NotFoundFailure404
import failures.CouponFailures._
import failures.PromotionFailures._
import failures.ObjectFailures._

object CouponManager {

  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[CouponFormResponse.Root] = (for {
    //guard to make sure the form is a coupon
    coupons ← * <~ Coupons.filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
    form  ← * <~ ObjectForms.mustFindById404(id)
  } yield CouponFormResponse.build(form)).run()

  def getShadow(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[CouponShadowResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    coupon  ← * <~ Coupons.filterByContextAndFormId(context.id, id).one.
      mustFindOr(CouponNotFoundForContext(id, contextName))
    shadow  ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
  } yield CouponShadowResponse.build(shadow)).run()

  def get(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[CouponResponse.Root] = (for {
      context  ← * <~ ObjectContexts.filterByName(contextName).one.
        mustFindOr(ObjectContextNotFound(contextName))
      coupon  ← * <~ Coupons.filterByContextAndFormId(context.id, id).one.
        mustFindOr(CouponNotFoundForContext(id, contextName))
      form     ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow   ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
  } yield CouponResponse.build(coupon, form, shadow)).run()

  def create(payload: CreateCoupon, contextName: String) 
    (implicit ec: EC, db: DB): Result[CouponResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    promotion ← * <~ Promotions.filterByContextAndFormId(context.id, payload.promotion).one.
      mustFindOr(PromotionNotFoundForContext(payload.promotion, context.name))
    form    ← * <~ ObjectForm(kind = Coupon.kind, attributes = payload.form.attributes)
    shadow  ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
    ins     ← * <~ ObjectUtils.insert(form, shadow)
    coupon ← * <~ Coupons.create(Coupon(contextId = context.id, 
      formId = ins.form.id, shadowId = ins.shadow.id, commitId = ins.commit.id,
      promotionId = payload.promotion))
  } yield CouponResponse.build(coupon, ins.form, ins.shadow)).runTxn()

  def update(id: Int, payload: UpdateCoupon, contextName: String)
    (implicit ec: EC, db: DB): Result[CouponResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    promotion ← * <~ Promotions.filterByContextAndFormId(context.id, payload.promotion).one.
      mustFindOr(PromotionNotFoundForContext(payload.promotion, context.name))
    coupon  ← * <~ Coupons.filterByContextAndFormId(context.id, id).one.
        mustFindOr(CouponNotFoundForContext(id, contextName))
    updatedCoupon ← * <~ ObjectUtils.update(coupon.formId, coupon.shadowId, 
      payload.form.attributes, payload.shadow.attributes)
    commit ← * <~ ObjectUtils.commit(updatedCoupon)
    coupon ← * <~ updateHead(coupon, payload.promotion, updatedCoupon.shadow, commit)
  } yield CouponResponse.build(coupon, updatedCoupon.form, updatedCoupon.shadow)).runTxn()

  def getIlluminated(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedCouponResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    result  ← * <~ getIlluminatedIntern(id, context)
  } yield result).run()

  def getIlluminatedByCode(code: String, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedCouponResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    couponCode ← * <~ CouponCodes.filter(_.code.toLowerCase === code.toLowerCase).one
      .mustFindOr(CouponWithCodeCannotBeFound(code))
    result  ← * <~ getIlluminatedIntern(couponCode.couponFormId, context)
  } yield result).run()

  def getIlluminatedIntern(id: Int, context: ObjectContext)
    (implicit ec: EC, db: DB): DbResultT[IlluminatedCouponResponse.Root] = for {
    coupon     ← * <~ Coupons.filter(_.contextId === context.id).
      filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
    form    ← * <~ ObjectForms.mustFindById404(coupon.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
  } yield IlluminatedCouponResponse.build(
    coupon = IlluminatedCoupon.illuminate(context, coupon, form, shadow)
    )

  def generateCode(id: Int, code: String)
    (implicit ec: EC, db: DB): Result[String] = (for {
    coupon  ← * <~ Coupons.filter(_.formId === id).one.
        mustFindOr(CouponNotFound(id))
    couponCode ← * <~ CouponCodes.create(CouponCode(couponFormId = id, code = code))
  } yield couponCode.code).runTxn()

  def generateCodes(id: Int, payload: GenerateCouponCodes)
    (implicit ec: EC, db: DB): Result[Seq[String]] = (for {
      _ ← * <~ validateCouponCodePayload(payload)
    coupon  ← * <~ Coupons.filter(_.formId === id).one.
        mustFindOr(CouponNotFound(id))
    codes ← * <~ CouponCodes.generateCodes(payload.prefix, payload.length, payload.quantity)
    unsavedCouponCodes = codes.map{c ⇒ CouponCode(couponFormId = id, code = c)}
    couponCodes ← * <~ CouponCodes.createAll(unsavedCouponCodes)
  } yield codes).runTxn()

  def getCodes(id: Int)
    (implicit ec: EC, db: DB): Result[Seq[CouponCodesResponse.Root]] = (for {
      coupons ← * <~ Coupons.filter(_.formId === id).one.mustFindOr(CouponNotFound(id))
      codes ← * <~ CouponCodes.filter(_.couponFormId === id).result
  } yield CouponCodesResponse.build(codes)).run()

  private def validateCouponCodePayload(p: GenerateCouponCodes)(implicit ec: EC) = {
    ObjectUtils.failIfErrors(
      Seq(
        if(p.quantity <= 0) Seq(CouponCodeQuanityMustBeGreaterThanZero) else Seq.empty,
        if(p.prefix.isEmpty) Seq(CouponCodePrefixNotSet) else Seq.empty,
        if(CouponCodes.isCharacterLimitValid(p.prefix.length, p.quantity, p.length)) Seq.empty
        else Seq(CouponCodeLengthIsTooSmall(p.prefix, p.quantity))
      ).flatten)
  }

  private def updateHead(coupon: Coupon, promotionId: Int, shadow: ObjectShadow, 
    maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Coupon] =
      maybeCommit match {
        case Some(commit) ⇒  for { 
          coupon   ← * <~ Coupons.update(coupon, coupon.copy(
            shadowId = shadow.id, commitId = commit.id, 
            promotionId = promotionId))
        } yield coupon
        case None ⇒  
          if(promotionId != coupon.promotionId) for { 
            coupon   ← * <~ Coupons.update(coupon, coupon.copy(promotionId = promotionId))
          } yield coupon
          else DbResultT.pure(coupon)
      }
}
