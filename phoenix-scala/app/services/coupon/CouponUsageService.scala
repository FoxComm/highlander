package services.coupon

import failures.CouponFailures._
import models.coupon._
import models.account.User
import models.objects.{ObjectContexts, ObjectForms}
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._

object CouponUsageService {

  private def couponUsageCount(couponFormId: Int, accountId: Int)(implicit ec: EC,
                                                                  db: DB): DBIO[Int] =
    for {
      coupon ← CouponCustomerUsages.filterByCouponAndAccount(couponFormId, accountId).one
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
      count ← * <~ couponCodeUsageCount(couponFormId, couponCodeId)
      _     ← * <~ failIf(usesAvailable <= count, CouponCodeCannotBeUsedAnymore(code))
    } yield {}

  def couponMustBeUsable(couponFormId: Int, accountId: Int, usesAvailable: Int, code: String)(
      implicit ec: EC,
      db: DB): DbResultT[Unit] =
    for {
      count ← * <~ couponUsageCount(couponFormId, accountId)
      _ ← * <~ failIf(usesAvailable <= count,
                      CouponCodeCannotBeUsedByCustomerAnymore(code, accountId))
    } yield {}

  def mustBeUsableByCustomer(couponFormId: Int,
                             couponCodeId: Int,
                             codeUsesAvailable: Int,
                             accountId: Int,
                             usesAvailableForCustomer: Int,
                             couponCode: String)(implicit ec: EC, db: DB): DbResultT[Unit] =
    for {
      _ ← * <~ couponCodeMustBeUsable(couponFormId,
                                      couponCodeId,
                                      usesAvailableForCustomer,
                                      couponCode)
      _ ← * <~ couponMustBeUsable(couponFormId, accountId, usesAvailableForCustomer, couponCode)
    } yield {}

  def updateUsageCounts(couponCodeId: Option[Int],
                        customer: User)(implicit ec: EC, db: DB, ctx: OC): DbResultT[Unit] = {
    couponCodeId match {
      case Some(codeId) ⇒
        for {
          couponCode ← * <~ CouponCodes.findById(codeId).extract.one.safeGet
          context    ← * <~ ObjectContexts.mustFindById400(ctx.id)
          code       ← * <~ CouponCodes.mustFindById400(codeId)
          coupon ← * <~ Coupons
            .filterByContextAndFormId(context.id, code.couponFormId)
            .one
            .mustFindOr(CouponNotFoundForContext(code.couponFormId, context.name))
          form ← * <~ ObjectForms.mustFindById400(coupon.formId)
          couponUsage ← * <~ CouponUsages
            .filterByCoupon(coupon.formId)
            .one
            .findOrCreate(
              CouponUsages.create(CouponUsage(couponFormId = coupon.formId, count = 1)))
          couponCodeUsage ← * <~ CouponCodeUsages
            .filterByCouponAndCode(coupon.formId, couponCode.id)
            .one
            .findOrCreate(
              CouponCodeUsages.create(
                CouponCodeUsage(couponFormId = coupon.formId,
                                couponCodeId = couponCode.id,
                                count = 0)))
          couponUsageByCustomer ← * <~ CouponCustomerUsages
            .filterByCouponAndAccount(coupon.formId, customer.accountId)
            .one
            .findOrCreate(
              CouponCustomerUsages.create(
                CouponCustomerUsage(couponFormId = coupon.formId,
                                    accountId = customer.accountId,
                                    count = 0)))
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
