
// @class CustomerCreditCards
// Accessible via [customerCreditCards](#foxapi-customercreditcards) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class CustomerCreditCards {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(customerId: Number): Promise<CreditCard[]>
   * List credit cards.
   */
  list(customerId) {
    return this.api.get(endpoints.customerCreditCards(customerId));
  }

  /**
   * @method add(customerId: Number, card: CreditCardCreatePayload): Promise<CreditCard>
   * Add credit card.
   */
  add(customerId, card) {
    return this.api.post(endpoints.customerCreditCards(customerId), card);
  }
}
