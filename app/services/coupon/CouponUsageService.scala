package services.coupon

import failures.CouponFailures._
import models.coupon._
import models.customer.Customer
import models.objects.{ObjectContexts, ObjectForms}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CouponUsageService {

  private def couponUsageCount(couponFormId: Int, customerId: Int)(implicit ec: EC,
                                                                   db: DB): DBIO[Int] =
    for {
      coupon ← CouponCustomerUsages.filterByCouponAndCustomer(couponFormId, customerId).one
    } yield coupon.fold(0)(_.count)

  private def couponCodeUsageCount(couponFormId: Int, couponCodeId: Int)(implicit ec: EC,
                                                                         db: DB): DBIO[Int] =
    for {
      counter ← CouponCodeUsages.filterByCouponAndCode(couponFormId, couponCodeId).one
    } yield counter.fold(0)(_.count)

  def couponCodeMustBeUsable(couponFormId: Int,
                             couponCodeId: Int,
                             usesAvailable: Int,
                             code: String)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      count ← * <~ couponCodeUsageCount(couponFormId, couponCodeId).toXor
      _ ← * <~ (if (count < usesAvailable) DbResult.failure(CouponCodeCannotBeUsedAnymore(code))
                else DbResult.unit)
    } yield {}

  def couponMustBeUsable(couponFormId: Int, customerId: Int, usesAvailable: Int, code: String)(
      implicit ec: EC,
      db: DB): DbResultT[Unit] =
    for {
      count ← * <~ couponUsageCount(couponFormId, customerId).toXor
      _ ← * <~ (if (count < usesAvailable)
                  DbResult.failure(CouponCodeCannotBeUsedByCustomerAnymore(code, customerId))
                else DbResult.unit)
    } yield {}

  def mustBeUsableByCustomer(couponFormId: Int,
                             couponCodeId: Int,
                             codeUsesAvailable: Int,
                             customerId: Int,
                             usesAvailableForCustomer: Int,
                             couponCode: String)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      _ ← * <~ couponCodeMustBeUsable(couponFormId,
                                      couponCodeId,
                                      usesAvailableForCustomer,
                                      couponCode)
      _ ← * <~ couponMustBeUsable(couponFormId, customerId, usesAvailableForCustomer, couponCode)
    } yield {}

  def updateUsageCounts(couponCodeId: Option[Int], contextId: Int, customer: Customer)(
      implicit ec: EC,
      db: DB): DbResultT[Unit] = {
    couponCodeId match {
      case Some(codeId) ⇒
        for {
          couponCode ← * <~ CouponCodes.findById(codeId).extract.one.safeGet.toXor
          context    ← * <~ ObjectContexts.mustFindById400(contextId)
          code       ← * <~ CouponCodes.mustFindById400(codeId)
          coupon ← * <~ Coupons
                    .filterByContextAndFormId(context.id, code.couponFormId)
                    .one
                    .mustFindOr(CouponNotFoundForContext(code.couponFormId, context.name))
          form ← * <~ ObjectForms.mustFindById400(coupon.formId)
          // TODO @anna: #longlivedbresultt
          couponUsage ← * <~ CouponUsages
                         .filterByCoupon(coupon.formId)
                         .one
                         .findOrCreate(CouponUsages
                               .create(new CouponUsage(couponFormId = coupon.formId, count = 1))
                               .value)
          couponCodeUsage ← * <~ CouponCodeUsages
                             .filterByCouponAndCode(coupon.formId, couponCode.id)
                             .one
                             .findOrCreate(
                                 CouponCodeUsages
                                   .create(new CouponCodeUsage(couponFormId = coupon.formId,
                                                               couponCodeId = couponCode.id,
                                                               count = 0))
                                   .value)
          couponUsageByCustomer ← * <~ CouponCustomerUsages
                                   .filterByCouponAndCustomer(coupon.formId, customer.id)
                                   .one
                                   .findOrCreate(
                                       CouponCustomerUsages
                                         .create(
                                             new CouponCustomerUsage(couponFormId = coupon.formId,
                                                                     customerId = customer.id,
                                                                     count = 0))
                                         .value)
          _ ← * <~ CouponUsages.update(couponUsage,
                                       couponUsage.copy(count = couponUsage.count + 1))
          _ ← * <~ CouponCodeUsages.update(couponCodeUsage,
                                           couponCodeUsage.copy(count = couponCodeUsage.count + 1))
          _ ← * <~ CouponCustomerUsages.update(
                 couponUsageByCustomer,
                 couponUsageByCustomer.copy(count = couponUsageByCustomer.count + 1))
        } yield {}
      case _ ⇒
        DbResultT.unit
    }
  }
}
