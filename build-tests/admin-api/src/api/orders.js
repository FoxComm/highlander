
// @class Orders
// Accessible via [orders](#foxapi-orders) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Orders {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(): Promise<FullOrder[]>
   * List orders.
   */
  list() {
    return this.api.get(endpoints.orders);
  }

  /**
   * @method get(referenceNumber: String): Promise<ValidationResult<FullOrder>>
   * @deprecated
   * Deprecated, use either `foxapi.orders.one` or `foxapi.cart.get`.
   */
  get(referenceNumber) {
    return this.api.get(endpoints.customerOrder(referenceNumber));
  }

  /**
   * @method one(referenceNumber: String): Promise<ValidationResult<FullOrder>>
   * Find order by reference number.
   */
  one(referenceNumber) {
    return this.api.get(endpoints.order(referenceNumber));
  }

  /**
   * @method update(referenceNumber: String, payload: OrderStatePayload): Promise<FullOrder>
   * Update order details.
   */
  update(referenceNumber, payload) {
    return this.api.patch(endpoints.order(referenceNumber), payload);
  }

  /**
   * @method increaseRemorsePeriod(referenceNumber: String): Promise<FullOrder>
   * Increase order remorse period.
   */
  increaseRemorsePeriod(referenceNumber) {
    return this.api.post(endpoints.orderIncreaseRemorsePeriod(referenceNumber));
  }

  /**
   * @method getWatchers(referenceNumber: String): Promise<Watcher[]>
   * Get order watchers.
   */
  getWatchers(referenceNumber) {
    return this.api.get(endpoints.orderWatchers(referenceNumber));
  }

  /**
   * @method addWatchers(referenceNumber: String, watchers: AddWatchersPayload): Promise<ValidationResult<Watcher[]>>
   * Add watchers to order.
   */
  addWatchers(referenceNumber, watchers) {
    return this.api.post(endpoints.orderWatchers(referenceNumber), watchers);
  }

  /**
   * @method removeWatcher(referenceNumber: String, watcherId: Number): Promise
   * Remove watcher from order.
   */
  removeWatcher(referenceNumber, watcherId) {
    return this.api.delete(endpoints.orderWatcher(referenceNumber, watcherId));
  }
}
