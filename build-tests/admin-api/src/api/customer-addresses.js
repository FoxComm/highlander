
// @class CustomerAddresses
// Accessible via [customerAddresses](#foxapi-customeraddresses) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class CustomerAddresses {
  constructor(api) {
    this.api = api;
  }

  /**
   * @method list(customerId: Number): Promise<Address[]>
   * List addresses.
   */
  list(customerId) {
    return this.api.get(endpoints.customerAddresses(customerId));
  }

  /**
   * @method one(customerId: Number, addressId: Number): Promise<Address>
   * Find address by id.
   */
  one(customerId, addressId) {
    return this.api.get(endpoints.customerAddress(customerId, addressId));
  }

  /**
   * @method add(customerId: Number, address: CreateAddressPayload): Promise<Address>
   * Add address.
   */
  add(customerId, address) {
    return this.api.post(endpoints.customerAddresses(customerId), address);
  }

  /**
   * @method update(customerId: Number, addressId: Number, address: UpdateAddressPayload): Promise<Address>
   * Update address details.
   */
  update(customerId, addressId, address) {
    return this.api.patch(endpoints.customerAddress(customerId, addressId), address);
  }

  /**
   * @method delete(customerId: Number, addressId: Number): Promise
   * Delete address.
   */
  delete(customerId, addressId) {
    return this.api.delete(endpoints.customerAddress(customerId, addressId));
  }
}
