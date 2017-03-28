
// @class Promotions
// Accessible via [promotions](#foxapi-promotions) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Promotions {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method create(context: String, promotion: PromotionPayload): Promise<PromotionIlluminated>
   * Create new promotion.
   */
  create(context, promotion) {
    return this.api.post(endpoints.promotions(context), promotion);
  }

  /**
   * @method one(context: String, promotionId: Number): Promise<PromotionIlluminated>
   * Find promotion by id.
   */
  one(context, promotionId) {
    return this.api.get(endpoints.promotion(context, promotionId));
  }

  /**
   * @method update(context: String, promotionId: Number, promotion: PromotionPayload): Promise<PromotionIlluminated>
   * Update promotion details.
   */
  update(context, promotionId, promotion) {
    return this.api.patch(endpoints.promotion(context, promotionId), promotion);
  }
}
