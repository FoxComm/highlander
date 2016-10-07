package models.coupon

case class CouponUsageRules(isExclusive: Boolean = false,
                            isUnlimitedPerCode: Boolean = false,
                            usesPerCode: Option[Int] = None,
                            isUnlimitedPerCustomer: Boolean = false,
                            usesPerCustomer: Option[Int] = None)
