
// @class CreditCards
// Accessible via [creditCards](#foxapi-creditcards) property of [FoxApi](#foxapi) instance.

import * as endpoints from '../endpoints';

export default class CreditCards {
  constructor(api) {
    this.api = api;
  }

  // @method list(): Promise<CreditCardsResponse>
  // Returns list of all credit cards.
  list() {
    return this.api.get(endpoints.creditCards);
  }
  
  // @method one(creditCardId: Number): Promise<CreditCard>
  // Returns credit card by id.
  one(creditCardId) {
    return this.api.get(endpoints.creditCard(creditCardId));
  }

  // @method add(creditCard: CreditCardCreatePayload): Promise<CreditCard>
  // Adds new credit card.
  add(creditCard) {
    return this.api.post(endpoints.creditCards, creditCard);
  }

  // @method update(creditCardId: Number, creditCard: CreditCardUpdatePayload): Promise<CreditCard>
  // Updates selected credit card.
  update(creditCardId, creditCard) {
    return this.api.patch(endpoints.creditCard(creditCardId), creditCard);
  }

  // @method setAsDefault(creditCardId: Number): Promise<CreditCard>
  // Sets selected credit card as default.
  setAsDefault(creditCardId) {
    return this.api.post(endpoints.creditCardDefault(creditCardId));
  }

  // @method delete(creditCardId: Number): Promise
  // Deletes selected credit card.
  delete(creditCardId) {
    return this.api.delete(endpoints.creditCard(creditCardId));
  }
}
