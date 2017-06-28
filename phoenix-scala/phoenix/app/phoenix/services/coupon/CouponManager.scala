package phoenix.services.coupon

import java.time.Instant

import cats._
import cats.implicits._
import cats.syntax._
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

  def create(
      payload: CreateCoupon,
      contextName: String,
      admin: Option[User])(implicit ec: EC, db: DB, ac: AC, au: AU): DbResultT[Seq[CouponResponse.Root]] =
    for {
      _ ← * <~ failIf(payload.singleCode.isEmpty && (payload.generateCodes.isEmpty || payload.generateCodes
                        .exists(_.quantity < 1)),
                      CouponCreationNoCodes)
      scope ← * <~ Scope.resolveOverride(payload.scope)
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      _ ← * <~ Promotions
           .filterByContextAndFormId(context.id, payload.promotion)
           .mustFindOneOr(PromotionNotFoundForContext(payload.promotion, context.name))
      codes: Seq[String] = payload.singleCode.toSeq ++ payload.generateCodes.toSeq.flatMap(gcc ⇒
        CouponCodes.generateCodes(gcc.prefix, gcc.length, gcc.quantity))
      couponFormAndShadow = FormAndShadow.fromPayload(Coupon.kind, forceActivate(payload.attributes))

      // create a form & shadow for each future CouponCode
      // FIXME: how to *effectively* create N forms & shadows? @michalrus
      formsAndShadows ← codes.toList.traverse(
                         _ ⇒
                           ObjectUtils
                             .insert(couponFormAndShadow.form, couponFormAndShadow.shadow, payload.schema))

      coupons = formsAndShadows.map { fas ⇒
        Coupon(scope = scope,
               contextId = context.id,
               formId = fas.form.id,
               shadowId = fas.shadow.id,
               commitId = fas.commit.id,
               promotionId = payload.promotion)
      }

      createdCoupons ← Coupons.createAllReturningModels(coupons)

      couponCodes = (codes zip formsAndShadows).map {
        case (code, fas) ⇒
          CouponCode(couponFormId = fas.form.id, code = code)
      }

      _ ← * <~ CouponCodes.createAll(couponCodes)

      response = (createdCoupons zip codes zip formsAndShadows).map {
        case ((coupon, code), fas) ⇒
          CouponResponse.build(context, code, coupon, fas.form, fas.shadow)
      }

      _ ← * <~ coupons.headOption.traverse { firstCoupon ⇒
           // TODO: Should they override scope? (`LogActivity().withScope(scope)`) @michalrus
           if (coupons.size > 1)
             LogActivity().multipleCouponCodesCreated(firstCoupon, admin)
           else
             LogActivity().singleCouponCodeCreated(firstCoupon, admin)
         }
    } yield response

  private def forceActivate(attributes: Map[String, Json]): Map[String, Json] =
    attributes
      .updated("activeFrom", ("t" → "datetime") ~ ("v" → Instant.ofEpochMilli(1).toString))
      .updated("activeTo", ("t" → "datetime") ~ ("v" → JNull))

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
      form ← * <~ ObjectForms.mustFindById404(coupon.formId)
      code ← CouponCodes
              .filter(_.couponFormId === form.id)
              .mustFindOneOr(CouponCodeNotFoundForCoupon(form.id))
      shadow ← * <~ ObjectShadows.mustFindById404(coupon.shadowId)
    } yield CouponResponse.build(context, code.code, coupon, form, shadow)

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
      code ← CouponCodes
              .filter(_.couponFormId === form.id)
              .mustFindOneOr(CouponCodeNotFoundForCoupon(form.id))
      shadow ← * <~ ObjectShadows.mustFindById404(archiveResult.shadowId)
    } yield CouponResponse.build(context, code.code, archiveResult, form, shadow)

  // FIXME: should be unused @michalrus @bagratinho
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
        else coupon.pure[DbResultT]
    }
}
