
// @class Dev
// Accessible via [dev](#foxapi-dev) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Dev {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method creditCardToken(creditCard: IssueCreditCardTokenPayload): Promise<CreditCardTokenResponse>
   * Issue credit card token.
   */
  creditCardToken(creditCard) {
    return this.api.post(endpoints.creditCardToken, creditCard);
  }
}
