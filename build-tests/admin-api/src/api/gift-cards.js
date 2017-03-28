
// @class GiftCards
// Accessible via [giftCards](#foxapi-giftcards) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class GiftCards {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(): Promise<GiftCard[]>
   * List gift cards.
   */
  list() {
    return this.api.get(endpoints.giftCards);
  }

  /**
   * @method create(giftCard: GiftCardCreatePayload): Promise<GiftCard>
   * Create gift card.
   */
  create(giftCard) {
    return this.api.post(endpoints.giftCards, giftCard);
  }

  /**
   * @method one(giftCardCode: String): Promise<GiftCard>
   * Find gift card by code.
   */
  one(giftCardCode) {
    return this.api.get(endpoints.giftCard(giftCardCode));
  }

  /**
   * @method update(giftCardCode: String, payload: GiftCardUpdateStatePayload): Promise<GiftCard>
   * Update gift card.
   */
  update(giftCardCode, payload) {
    return this.api.patch(endpoints.giftCard(giftCardCode), payload);
  }
}
