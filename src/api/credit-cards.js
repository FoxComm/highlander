

import * as endpoints from '../endpoints';

export function getCreditCards() {
  return this.get(endpoints.creditCards);
}

