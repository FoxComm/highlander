package services.discount

import cats.implicits._
import failures.DiscountFailures._
import failures.NotFoundFailure404
import failures.ObjectFailures._
import models.discount._
import models.objects._
import models.account._
import payloads.DiscountPayloads._
import responses.DiscountResponses._
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object DiscountManager {

  def getForm(id: Int)(implicit ec: EC, db: DB): DbResultT[DiscountFormResponse.Root] =
    for {
      // guard to make sure the form is a discount
      _    ← * <~ Discounts.filter(_.formId === id).mustFindOneOr(NotFoundFailure404(Discount, id))
      form ← * <~ ObjectForms.mustFindById404(id)
    } yield DiscountFormResponse.build(form)

  def getShadow(id: Int, contextName: String)(implicit ec: EC,
                                              db: DB): DbResultT[DiscountShadowResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      discount ← * <~ Discounts
                  .filter(_.contextId === context.id)
                  .filter(_.formId === id)
                  .mustFindOneOr(NotFoundFailure404(Discount, id))
      shadow ← * <~ ObjectShadows.mustFindById404(discount.shadowId)
    } yield DiscountShadowResponse.build(shadow)

  def get(discountId: Int, contextName: String)(implicit ec: EC,
                                                db: DB): DbResultT[DiscountResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      discount ← * <~ Discounts
                  .filter(_.contextId === context.id)
                  .filter(_.formId === discountId)
                  .mustFindOneOr(DiscountNotFoundForContext(discountId, context.id))
      form   ← * <~ ObjectForms.mustFindById404(discount.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(discount.shadowId)
    } yield DiscountResponse.build(form, shadow)

  def create(
      payload: CreateDiscount,
      contextName: String)(implicit ec: EC, db: DB, au: AU): DbResultT[DiscountResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      discount ← * <~ createInternal(payload, context)
    } yield DiscountResponse.build(discount.form, discount.shadow)

  case class CreateInternalResult(discount: Discount,
                                  commit: ObjectCommit,
                                  form: ObjectForm,
                                  shadow: ObjectShadow)
      extends FormAndShadow {
    override def update(form: ObjectForm, shadow: ObjectShadow): FormAndShadow =
      copy(form = form, shadow = shadow)
  }

  def createInternal(payload: CreateDiscount, context: ObjectContext)(
      implicit ec: EC,
      au: AU): DbResultT[CreateInternalResult] = {
    val formAndShadow = FormAndShadow.fromPayload(Discount.kind, payload.attributes)

    for {
      scope ← * <~ Scope.resolveOverride(payload.scope)
      ins   ← * <~ ObjectUtils.insert(formAndShadow.form, formAndShadow.shadow, payload.schema)
      discount ← * <~ Discounts.create(
                    Discount(scope = scope,
                             contextId = context.id,
                             formId = ins.form.id,
                             shadowId = ins.shadow.id,
                             commitId = ins.commit.id))
    } yield CreateInternalResult(discount, ins.commit, ins.form, ins.shadow)
  }

  def update(discountId: Int, payload: UpdateDiscount, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[DiscountResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      discount ← * <~ updateInternal(discountId, payload.attributes, context)
    } yield DiscountResponse.build(discount.form, discount.shadow)

  case class UpdateInternalResult(oldDiscount: Discount,
                                  discount: Discount,
                                  form: ObjectForm,
                                  shadow: ObjectShadow)

  def updateInternal(
      discountId: Int,
      attributes: Map[String, Json],
      context: ObjectContext,
      forceUpdate: Boolean = false)(implicit ec: EC, db: DB): DbResultT[UpdateInternalResult] = {

    val formAndShadow = FormAndShadow.fromPayload(Discount.kind, attributes)

    for {
      discount ← * <~ Discounts
                  .filter(_.contextId === context.id)
                  .filter(_.formId === discountId)
                  .mustFindOneOr(DiscountNotFoundForContext(discountId, context.id))
      update ← * <~ ObjectUtils.update(discount.formId,
                                       discount.shadowId,
                                       formAndShadow.form.attributes,
                                       formAndShadow.shadow.attributes,
                                       forceUpdate)
      commit  ← * <~ ObjectUtils.commit(update)
      updated ← * <~ updateHead(discount, update.shadow, commit)
    } yield UpdateInternalResult(discount, updated, update.form, update.shadow)
  }

  def getIlluminated(id: Int, contextName: String)(
      implicit ec: EC,
      db: DB): DbResultT[IlluminatedDiscountResponse.Root] =
    for {
      context ← * <~ ObjectContexts
                 .filterByName(contextName)
                 .mustFindOneOr(ObjectContextNotFound(contextName))
      discount ← * <~ Discounts
                  .filter(_.contextId === context.id)
                  .filter(_.formId === id)
                  .mustFindOneOr(NotFoundFailure404(Discount, id))
      form   ← * <~ ObjectForms.mustFindById404(discount.formId)
      shadow ← * <~ ObjectShadows.mustFindById404(discount.shadowId)
    } yield
      IlluminatedDiscountResponse.build(
          IlluminatedDiscount.illuminate(context = context.some, form, shadow))

  private def updateHead(discount: Discount,
                         shadow: ObjectShadow,
                         maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Discount] =
    maybeCommit match {
      case Some(commit) ⇒
        Discounts.update(discount, discount.copy(shadowId = shadow.id, commitId = commit.id))
      case None ⇒
        DbResultT.good(discount)
    }
}
