
// @namespace FoxApi

import * as endpoints from '../endpoints';

// @method getCreditCards(): Promise<CreditCardsResponse>
export function getCreditCards() {
  return this.get(endpoints.creditCards);
}

