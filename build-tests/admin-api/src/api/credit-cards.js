// @class CreditCards
// Accessible via [creditCards](#foxapi-creditcards) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';
import { isBrowser, loadScript } from '../utils/browser';

export default class CreditCards {
  constructor(api) {
    this.api = api;

    if (isBrowser()) {
      // load Stripe.js
      loadScript('https://js.stripe.com/v2/').then(() => {
        Stripe.setPublishableKey(this.api.stripe_key);
      });
    }
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

  // @method create(creditCard: CreditCardCreatePayload, billingAddress: BillingAddressCreatePayload, addressIsNew: Boolean): Promise<CreditCard>
  // Adds new credit card.
  create(creditCard, billingAddress, addressIsNew) {
    return new Promise((resolve, reject) => {
      Stripe.card.createToken(creditCardForStripePayload(creditCard, billingAddress), (status, response) => {
        if (response.error) {
          reject([response.error.message]);
        } else {
          return createCardFromStripeToken(response, billingAddress, addressIsNew)
          .then(response => resolve(response))
          .catch(err => reject(err));
        }
      });
    });
  }

  createCardFromStripeToken(token, billingAddress, addressIsNew) {
    return new Promise((resolve, reject) => {
          var payload = creditCardFromStripePayload(token, billingAddress, addressIsNew);

          return this.api.post(endpoints.creditCards, payload)
            .then(response => resolve(response))
            .catch(err => !!err.responseJson.errors ? reject(err.responseJson.errors) : reject([err.message]));
    });
  }

  // @method update(creditCardId: Number, creditCard: CreditCardUpdatePayload): Promise<CreditCard>
  // Updates selected credit card.
  update(creditCardId, creditCard) {
    return this.api.patch(endpoints.creditCard(creditCardId), creditCard);
  }

  // @method setAsDefault(creditCardId: Number): Promise<CreditCard>
  // Sets selected credit card as default.
  setAsDefault(creditCardId, isDefault = true) {
    return this.api.post(endpoints.creditCardDefault(creditCardId), { isDefault: isDefault });
  }

  // @method delete(creditCardId: Number): Promise
  // Deletes selected credit card.
  delete(creditCardId) {
    return this.api.delete(endpoints.creditCard(creditCardId));
  }

  // @method cardType(number: String): String
  // Detects credit card type
  cardType(number = '') {
    return Stripe.card.cardType(number);
  }

  // @method validateCardNumber(number: String): Boolean
  // Check if credit card'c number is valid
  validateCardNumber(number = '') {
    return Stripe.card.validateCardNumber(number);
  }

  // @method validateCVC(cvc: String|Number): Boolean
  // Check if credit card's cvc is valid
  validateCVC(cvc = '') {
    return Stripe.card.validateCVC(cvc);
  }

  // @method validateExpiry(month: String|Number, year: String|Number): Boolean
  // Check if credit card's valid thru date is valid
  validateExpiry(month = '', year = '') {
    return Stripe.card.validateExpiry(month, year);
  }
}

function creditCardForStripePayload(creditCard, billingAddress) {
  return {
    name: creditCard.holderName,
    number: creditCard.number,
    cvc: creditCard.cvc,
    exp_month: creditCard.expMonth,
    exp_year: creditCard.expYear,
    address_line1: billingAddress.address1,
    address_line2: billingAddress.address2,
    address_zip: billingAddress.zip,
    address_city: billingAddress.city,
    address_state: billingAddress.state,
    address_country: billingAddress.country,
  }
}

function creditCardFromStripePayload(stripeResponse, billingAddress, addressIsNew) {
  return {
    token: stripeResponse.id,
    holderName: stripeResponse.card.name,
    lastFour: stripeResponse.card.last4,
    expMonth: stripeResponse.card.exp_month,
    expYear: stripeResponse.card.exp_year,
    brand: stripeResponse.card.brand,
    addressIsNew: addressIsNew,
    billingAddress,
  }
}
