import HashStore from '../lib/hash-store';

class CreditCardsStore extends HashStore {
  baseUri(customerId) {
    return `/customers/${customerId}/payment-methods/credit-cards`;
  }
}

export default new CreditCardsStore();
