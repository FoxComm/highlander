
// @class Coupons
// Accessible via [coupons](#foxapi-coupons) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Coupons {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method create(context: String, coupon: CouponPayload): Promise<Coupon>
   * Create new coupon.
   */
  create(context, coupon) {
    return this.api.post(endpoints.coupons(context), coupon);
  }

  /**
   * @method one(context: String, couponIdOrCode: Number | String): Promise<Coupon>
   * Find coupon by id or code.
   */
  one(context, couponIdOrCode) {
    return this.api.get(endpoints.coupon(context, couponIdOrCode));
  }

  /**
   * @method update(context: String, couponId: Number, coupon: CouponPayload): Promise<Coupon>
   * Update coupon details.
   */
  update(context, couponId, coupon) {
    return this.api.patch(endpoints.coupon(context, couponId), coupon);
  }
}
