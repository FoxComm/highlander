package phoenix.models.coupon

case class CouponUsageRules(isUnlimitedPerCode: Boolean = false,
                            usesPerCode: Option[Int] = None,
                            isUnlimitedPerCustomer: Boolean = false,
                            usesPerCustomer: Option[Int] = None)
