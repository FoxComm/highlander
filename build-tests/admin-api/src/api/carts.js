
// @class Carts
// Accessible via [carts](#foxapi-carts) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Carts {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method one(referenceNumber: String): Promise<ValidationResult<FullOrder>>
   * Find cart by reference number.
   */
  one(referenceNumber) {
    return this.api.get(endpoints.cart(referenceNumber));
  }

  /**
   * @method getShippingMethods(referenceNumber: String): Promise<ShippingMethod[]>
   * Retrieve cart's shipping methods.
   */
  getShippingMethods(referenceNumber) {
    return this.api.get(endpoints.cartShippingMethods(referenceNumber));
  }

  /**
   * @method updateLineItemQuantities(referenceNumber: String, lineItems: UpdateLineItemsPayload[]): Promise<ValidationResult<FullOrder>>
   * Update cart's line item quantities.
   */
  updateLineItemQuantities(referenceNumber, lineItems) {
    return this.api.post(endpoints.cartLineItems(referenceNumber), lineItems);
  }

  /**
   * @method addLineItemQuantities(referenceNumber: String, lineItems: UpdateLineItemsPayload[]): Promise<ValidationResult<FullOrder>>
   * Add cart's line item quantities.
   */
  addLineItemQuantities(referenceNumber, lineItems) {
    return this.api.patch(endpoints.cartLineItems(referenceNumber), lineItems);
  }

  /**
   * @method getWatchers(referenceNumber: String): Promise<Watcher[]>
   * List notes.
   */
  getWatchers(referenceNumber) {
    return this.api.get(endpoints.cartWatchers(referenceNumber));
  }

  /**
   * @method addWatchers(referenceNumber: String, watchers: AddWatchersPayload): Promise<ValidationResult<Watcher[]>>
   * List notes.
   */
  addWatchers(referenceNumber, watchers) {
    return this.api.post(endpoints.cartWatchers(referenceNumber), watchers);
  }

  /**
   * @method removeWatcher(referenceNumber: String, watcherId: Number): Promise
   * List notes.
   */
  removeWatcher(referenceNumber, watcherId) {
    return this.api.delete(endpoints.cartWatcher(referenceNumber, watcherId));
  }
}

/*
 * @miniclass ValidationResult (FoxApi)
 * @field result: <T>
 * request's result
 *
 * @field warnings: String[]
 * list of validation warnings
 */
