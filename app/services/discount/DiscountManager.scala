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
import payloads.{CreateDiscountForm, CreateDiscountShadow, UpdateDiscountForm, UpdateDiscountShadow}
import utils.aliases._
import cats.data.NonEmptyList
import failures.NotFoundFailure404
import failures.DiscountFailures._
import failures.ObjectFailures._

object DiscountManager {

  // Detailed info for SKU of each type in given warehouse
  def getForm(id: Int)
    (implicit ec: EC, db: DB): Result[DiscountFormResponse.Root] = (for {
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

  def getIlluminatedDiscount(id: Int, contextName: String)
    (implicit ec: EC, db: DB): Result[IlluminatedDiscountResponse.Root] = (for {
    context ← * <~ ObjectContexts.filterByName(contextName).one.
      mustFindOr(ObjectContextNotFound(contextName))
    discount     ← * <~ Discounts.filter(_.contextId === context.id).
      filter(_.formId === id).one.mustFindOr(DiscountNotFound(id))
    form    ← * <~ ObjectForms.mustFindById404(discount.formId)
    shadow  ← * <~ ObjectShadows.mustFindById404(discount.shadowId)
  } yield IlluminatedDiscountResponse.build(IlluminatedDiscount.illuminate(
    context, discount, form, shadow))).run()

  def validateShadow(discount: Discount, form: ObjectForm, shadow: ObjectShadow) 
  (implicit ec: EC, db: DB) : DbResultT[Unit] = 
    DiscountValidator.validate(discount, form, shadow) match {
      case Nil ⇒ DbResultT.pure(Unit)
      case head ::tail ⇒ DbResultT.leftLift(NonEmptyList(head, tail))
    }

}
