
// @class Addresses
// Accessible via [addresses](#foxapi-addresses) property of [FoxApi](#foxapi) instance.

import endpoints from '../endpoints';

export default class Addresses {
  constructor(api) {
    this.api = api;
  }

  // @method list(options: Query options): Promise<AddressesResponse>
  // Choices for `options.sortBy` parameter:
  //
  // <ul class="collapsible collapsed">
  //   <li>`id`</li>
  //   <li>`regionId`</li>
  //   <li>`name`</li>
  //   <li>`address1`</li>
  //   <li>`address2`</li>
  //   <li>`city`</li>
  //   <li>`zip`</li>
  //   <li>`isDefaultShipping`</li>
  //   <li>`phoneNumber`</li>
  //   <li>`deletedAt`</li>
  //   <li>`region_id`</li>
  //   <li>`region_countryId`</li>
  //   <li>`region_name`</li>
  //   <li>`region_abbreviation`</li>
  // </ul>
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
