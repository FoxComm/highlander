package phoenix.services.coupon

import java.time.Instant

import core.db._
import core.failures.NotFoundFailure404
import objectframework.ObjectFailures._
import objectframework.ObjectUtils
import objectframework.models._
import org.json4s.Formats
import org.json4s.JsonAST._
import org.json4s.JsonDSL._
import phoenix.failures.CouponFailures._
import phoenix.failures.PromotionFailures._
import phoenix.models.account._
import phoenix.models.coupon._
import phoenix.models.promotion._
import phoenix.payloads.CouponPayloads._
import phoenix.responses.CouponResponses._
import phoenix.services.LogActivity
import phoenix.utils.JsonFormatters
import phoenix.utils.aliases._
import slick.jdbc.PostgresProfile.api._

object CouponManager {

  implicit val formats: Formats = JsonFormatters.phoenixFormats

  def create(payload: CreateCoupon,
             contextName: String,
             admin: Option[User])(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[CouponResponse.Root] = {

    val formAndShadow = FormAndShadow.fromPayload(Coupon.kind, forceActivate(payload.attributes))

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      _ ← * <~ Promotions
           .filterByContextAndFormId(context.id, payload.promotion)
           .mustFindOneOr(PromotionNotFoundForContext(payload.promotion, context.name))
      ins ← * <~ ObjectUtils.insert(formAndShadow.form, formAndShadow.shadow, payload.schema)
      coupon ← * <~ Coupons.create(
                Coupon(scope = scope,
                       contextId = context.id,
                       formId = ins.form.id,
                       shadowId = ins.shadow.id,
                       commitId = ins.commit.id,
                       promotionId = payload.promotion))
      response = CouponResponse.build(context, coupon, ins.form, ins.shadow)
      _ ← * <~ LogActivity().withScope(scope).couponCreated(response, admin)
    } yield response
  }

  private def forceActivate(attributes: Map[String, Json]): Map[String, Json] =
    attributes
      .updated("activeFrom", ("t" → "datetime") ~ ("v" → Instant.ofEpochMilli(1).toString))
      .updated("activeTo", ("t" → "datetime") ~ ("v" → JNull))

  def update(id: Int, payload: UpdateCoupon, contextName: String, admin: User)(
      implicit ec: EC,
      db: DB,
      ac: AC): DbResultT[CouponResponse.Root] = {

    val formAndShadow = FormAndShadow.fromPayload(Coupon.kind, forceActivate(payload.attributes))

    for {
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
                                        formAndShadow.form.attributes,
                                        formAndShadow.shadow.attributes)
      commit ← * <~ ObjectUtils.commit(updated)
      coupon ← * <~ updateHead(coupon, payload.promotion, updated.shadow, commit)
      response = CouponResponse.build(context, coupon, updated.form, updated.shadow)
      _ ← * <~ LogActivity().couponUpdated(response, Some(admin))
    } yield response
  }

  def getIlluminated(id: Int, contextName: String)(implicit ec: EC, db: DB): DbResultT[CouponResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      result ← * <~ getIlluminatedIntern(id, context)
    } yield result

  def getIlluminatedByCode(code: String, contextName: String)(implicit ec: EC,
                                                              db: DB): DbResultT[CouponResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      couponCode ← * <~ CouponCodes
                    .filter(_.code.toLowerCase === code.toLowerCase)
                    .mustFindOneOr(CouponWithCodeCannotBeFound(code))
      result ← * <~ getIlluminatedIntern(couponCode.couponFormId, context)
    } yield result

  private def getIlluminatedIntern(id: Int, context: ObjectContext)(implicit ec: EC,
                                                                    db: DB): DbResultT[CouponResponse.Root] =
    for {
      coupon ← * <~ Coupons
                .filter(_.contextId === context.id)
                .filter(_.formId === id)
                .mustFindOneOr(CouponNotFound(id))
      form   ← * <~ ObjectForms.mustFindById404(coupon.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield CouponResponse.build(context, coupon, form, shadow)

  def archiveByContextAndId(contextName: String, formId: Int)(implicit ec: EC,
                                                              db: DB): DbResultT[CouponResponse.Root] =
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
    } yield CouponResponse.build(context, archiveResult, form, shadow)

  def generateCode(id: Int, code: String, admin: User)(implicit ec: EC, db: DB, ac: AC): DbResultT[String] =
    for {
      coupon     ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      couponCode ← * <~ CouponCodes.create(CouponCode(couponFormId = id, code = code))
      _          ← * <~ LogActivity().singleCouponCodeCreated(coupon, Some(admin))
    } yield couponCode.code

  def generateCodes(id: Int, payload: GenerateCouponCodes, admin: User)(implicit ec: EC,
                                                                        db: DB,
                                                                        ac: AC): DbResultT[Seq[String]] =
    for {
      _         ← * <~ validateCouponCodePayload(payload)
      coupon    ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      generated ← * <~ CouponCodes.generateCodes(payload.prefix, payload.length, payload.quantity)
      unsaved = generated.map { c ⇒
        CouponCode(couponFormId = id, code = c)
      }
      _ ← * <~ CouponCodes.createAll(unsaved)
      _ ← * <~ LogActivity().multipleCouponCodeCreated(coupon, Some(admin))
    } yield generated

  def getCodes(id: Int)(implicit ec: EC, db: DB): DbResultT[Seq[CouponCodesResponse.Root]] =
    for {
      _     ← * <~ Coupons.filter(_.formId === id).mustFindOneOr(CouponNotFound(id))
      codes ← * <~ CouponCodes.filter(_.couponFormId === id).result
    } yield CouponCodesResponse.build(codes)

  private def validateCouponCodePayload(p: GenerateCouponCodes)(implicit ec: EC) =
    ObjectUtils.failIfErrors(
      Seq(
        if (p.quantity <= 0) Seq(CouponCodeQuanityMustBeGreaterThanZero)
        else Seq.empty,
        if (p.prefix.isEmpty) Seq(CouponCodePrefixNotSet) else Seq.empty,
        if (CouponCodes.isCharacterLimitValid(p.prefix.length, p.quantity, p.length))
          Seq.empty
        else Seq(CouponCodeLengthIsTooSmall(p.prefix, p.quantity))
      ).flatten)

  private def updateHead(coupon: Coupon,
                         promotionId: Int,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Coupon] =
    maybeCommit match {
      case Some(commit) ⇒
        Coupons.update(coupon,
                       coupon.copy(shadowId = shadow.id, commitId = commit.id, promotionId = promotionId))
      case None ⇒
        if (promotionId != coupon.promotionId)
          Coupons.update(coupon, coupon.copy(promotionId = promotionId))
        else DbResultT.good(coupon)
    }
}
