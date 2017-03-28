
// @class StoreCredits
// Accessible via [storeCredits](#foxapi-storecredits) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class StoreCredits {
  constructor(api) {
    this.api = api;
  }

  // @method list(): Promise<StoreCreditsResponse>
  // Returns list of all store credits.
  list() {
    return this.api.post(endpoints.storeCredits, {
      query: {
        term: {
          customerId: this.api.getCustomerId()
        }
      }
    });
  }

  // @method one(storeCreditId: Number): Promise<StoreCredit>
  // Returns store credit by id.
  one(storeCreditId) {
    return this.api.get(endpoints.storeCredit(storeCreditId));
  }

  // @method totals(): Promise<StoreCreditTotals>
  totals() {
    return this.api.get(endpoints.storeCreditTotals);
  }
}

// @miniclass StoreCreditsResponse (StoreCredits)
// @inherits ResultMetadata
// @field result: Array<StoreCredit>

