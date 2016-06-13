
// @class Addresses
// Accessible via [addresses](#foxapi-addresses) property of [FoxApi](#foxapi) instance.

import * as endpoints from '../endpoints';

export default class Addresses {
  constructor(api) {
    this.api = api;
  }

  // @method list(options: Query options): Promise<AddressesResponse>
  // Choices for `options.sortBy` parameter:
  // 
  // - `id`
  // - `regionId`
  // - `name`
  // - `address1`
  // - `address2`
  // - `city`
  // - `zip`
  // - `isDefaultShipping`
  // - `phoneNumber`
  // - `deletedAt`
  // - `region_id`
  // - `region_countryId`
  // - `region_name`
  // - `region_abbreviation`
  list(options) {
    return this.api.get(endpoints.addresses, options);
  }

  // @method one(addressId: Number): Promise<Address>
  one(addressId) {
    return this.api.get(endpoints.address(addressId));
  }

  // @method add(address: CreateAddressPayload): Promise<Address>
  // Adds new address.
  add(address) {
    return this.api.post(endpoints.addresses, address)
  }

  // @method update(addressId: Number, address: UpdateAddressPayload): Promise<Address>
  // Updates selected address.
  update(addressId, address) {
    return this.api.patch(endpoints.address(addressId), address);
  }

  // @method setAsDefault(addressId: Number): Promise<Address>
  // Sets selected address as default.
  setAsDefault(addressId) {
    return this.api.post(endpoints.addressDefault(addressId));
  }

  // @method removeDefault(): Promise
  // Removes default address.
  removeDefault() {
    return this.api.delete(endpoints.addressesDefault);
  }

  // @method delete(addressId: Number): Promise
  // Deletes address.
  delete(addressId) {
    return this.api.delete(endpoints.address(addressId));
  }
}
