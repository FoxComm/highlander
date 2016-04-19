package services.discount

import services.Result

import models.objects._
import models.discount._
import responses.DiscountResponses._
import utils.DbResultT
import utils.DbResultT._
import utils.DbResultT.implicits._
import utils.Slick.implicits._
import slick.driver.PostgresDriver.api._
import payloads.{CreateDiscount, CreateDiscountForm, CreateDiscountShadow, 
  UpdateDiscount, UpdateDiscountForm, UpdateDiscountShadow}
import utils.aliases._
import cats.data.NonEmptyList
import cats.implicits._
import failures.NotFoundFailure404
import failures.DiscountFailures._
import failures.ObjectFailures._

object DiscountManager {

  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[DiscountFormResponse.Root] = (for {
    //guard to make sure the form is a discount
    discounts ← * <~ Discounts.filter(_.formId === id).one.mustFindOr(DiscountNotFound(id))
    form  ← * <~ ObjectForms.mustFindById404(id)
  } yield DiscountFormResponse.build(form)).run()

  def getShadow(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[DiscountShadowResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    discount     ← * <~ Discounts.filter(_.contextId === context.id).
      filter(_.formId === id).one.mustFindOr(DiscountNotFound(id))
    shadow  ← * <~ ObjectShadows.mustFindById404(discount.shadowId)
  } yield DiscountShadowResponse.build(shadow)).run()

  def get(discountId: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[DiscountResponse.Root] = (for {
      context  ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
      discount ← * <~ Discounts.filter(_.contextId === context.id).
      filter(_.formId === discountId).one.mustFindOr(
        DiscountNotFoundForContext(discountId, context.id)) 
      form     ← * <~ ObjectForms.mustFindById404(discount.formId)
      shadow   ← * <~ ObjectShadows.mustFindById404(discount.shadowId)
  } yield DiscountResponse.build(form, shadow)).run()

  def create(payload: CreateDiscount, contextName: String) 
    (implicit ec: EC, db: DB): Result[DiscountResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    r ← * <~ createInternal(payload, context)
  } yield DiscountResponse.build(r.form, r.shadow)).runTxn()

  case class CreateInternalResult(discount: Discount, commit: ObjectCommit, 
    form: ObjectForm, shadow: ObjectShadow)

  def createInternal(payload: CreateDiscount, context: ObjectContext)
    (implicit ec: EC): DbResultT[CreateInternalResult] = for {
    form    ← * <~ ObjectForm(kind = Discount.kind, attributes = 
      payload.form.attributes)
    shadow  ← * <~ ObjectShadow(attributes = payload.shadow.attributes)
    ins     ← * <~ ObjectUtils.insert(form, shadow)
    discount ← * <~ Discounts.create(Discount(contextId = context.id, 
      formId = ins.form.id, shadowId = ins.shadow.id, commitId = ins.commit.id))
  } yield CreateInternalResult(discount, ins.commit, ins.form, ins.shadow)


  def update(discountId: Int, payload: UpdateDiscount, contextName: String)
    (implicit ec: EC, db: DB): Result[DiscountResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
      r ← * <~ updateInternal(discountId, payload, context)
  } yield DiscountResponse.build(r.form, r.shadow)).runTxn()

  case class UpdateInternalResult(oldDiscount: Discount, discount: Discount, form: ObjectForm, 
    shadow: ObjectShadow)

  def updateInternal(discountId: Int, payload: UpdateDiscount, context: ObjectContext, 
    forceUpdate: Boolean = false)
    (implicit ec: EC, db: DB): DbResultT[UpdateInternalResult] = for {
    discount ← * <~ Discounts.filter(_.contextId === context.id).
      filter(_.formId === discountId).one.mustFindOr(
        DiscountNotFoundForContext(discountId, context.id)) 
    updatedDiscount ← * <~ ObjectUtils.update(discount.formId, discount.shadowId, 
      payload.form.attributes, payload.shadow.attributes, forceUpdate)
    commit ← * <~ ObjectUtils.commit(updatedDiscount)
    updated ← * <~ updateHead(discount, updatedDiscount.shadow, commit)
  } yield UpdateInternalResult(discount, updated, updatedDiscount.form, updatedDiscount.shadow)

  def getIlluminated(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedDiscountResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    discount     ← * <~ Discounts.filter(_.contextId === context.id).
      filter(_.formId === id).one.mustFindOr(DiscountNotFound(id))
    form    ← * <~ ObjectForms.mustFindById404(discount.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(discount.shadowId)
  } yield IlluminatedDiscountResponse.build(IlluminatedDiscount.illuminate(
    context = context.some, form, shadow))).run()

  private def updateHead(discount: Discount, shadow: ObjectShadow, 
    maybeCommit: Option[ObjectCommit])(implicit ec: EC): DbResultT[Discount] =
      maybeCommit match {
        case Some(commit) ⇒  for { 
          discount   ← * <~ Discounts.update(discount, discount.copy(
            shadowId = shadow.id, commitId = commit.id))
        } yield discount
        case None ⇒ DbResultT.pure(discount)
      }
}
