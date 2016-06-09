
// @namespace FoxApi

import * as endpoints from '../endpoints';

// @method getCreditCards(): Promise<CreditCards>
export function getCreditCards() {
  return this.get(endpoints.creditCards);
}

