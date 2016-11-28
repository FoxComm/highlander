const isServer = typeof self === 'undefined';

class StripeApi {

  constructor(key) {
    this.key = key;
    this.initialized = false;
  }

  init() {
    this.initialized = true;
    Stripe.setPublishableKey(this.key);
  }

  @checkKey
  addCreditCard(creditCard, billingAddress, addressIsNew) {
    const creditCardPayload = creditCardForStripePayload(creditCard, billingAddress);

    return new Promise((resolve, reject) => {
      Stripe.card.createToken(creditCardPayload, (status, response) => {
        if (!response.error) {
          resolve(creditCardFromStripePayload(response, billingAddress, addressIsNew));
        } else {
          reject(response.error.message);
        }
      });
    });
  }
}

function checkKey(target, name, descriptor) {
  const method = descriptor.value;

  descriptor.value = function (...args) {
    if (isServer) {
      return;
    }

    if (Stripe == 'undefined') {
      console.warn('Stripe.js script not loaded yet');

      return;
    }

    if (!this.initialized) {
      this.init();
    }

    return method(...args);
  };
}

function creditCardForStripePayload(creditCard, billingAddress) {
  return {
    name: creditCard.holderName,
    number: creditCard.cardNumber,
    cvc: creditCard.cvv,
    exp_month: creditCard.expMonth,
    exp_year: creditCard.expYear,
    address_line1: billingAddress.address1,
    address_line2: billingAddress.address2,
    address_zip: billingAddress.zip,
    address_city: billingAddress.city,
    address_state: billingAddress.state,
    address_country: billingAddress.country,
  };
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
  };
}

export default new StripeApi(isServer ? process.env.STRIPE_PUBLISHABLE_KEY : STRIPE_PUBLISHABLE_KEY);
