
// @class CouponCodes
// Accessible via [couponcodes](#foxapi-couponcodes) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class CouponCodes {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(couponId: Number): Promise<CouponCode[]>
   * List coupon codes.
   */
  list(couponId) {
    return this.api.get(endpoints.couponCodes(couponId));
  }

  /**
   * @method generate(couponId: Number, payload: GenerateCodesPayload): Promise<String[]>
   * Generate coupon codes.
   */
  generate(couponId, payload) {
    return this.api.post(endpoints.couponCodesGenerate(couponId), payload);
  }
}
