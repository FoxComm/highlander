// @namespace FoxApi

import * as endpoints from '../endpoints';

// @method getAddresses(options: Query options): Promise
export function getAddresses(options) {
  return this.get(endpoints.addresses, options);
}

// @method getAddress(addressId: Number): Promise
export function getAddress(addressId) {
  return this.get(endpoints.address(addressId))
}

// @method addAddress(address: Address): Promise
// Adds new address
export function addAddress(address) {
  return this.post(endpoints.addresses, address)
}

// @method editAddress(addressId: Number, address: Address): Promise
// Updates selected address
export function editAddress(addressId, address) {
  return this.patch(endpoints.address(addressId), address);
}

// @method setAddressAsDefaut(addressId: Number): Promise
// Sets selected address as default.
export function setAddressAsDefaut(addressId) {
  return this.post(endpoints.addressDefault(addressId));
}

// @method removeDefaultAddress(): Promise
// Removes default address.
export function removeDefaultAddress() {
  return this.delete(endpoints.addressesDefault);
}

// @method deleteAddress(addressId: Number): Promise
// Deletes address.
export function deleteAddress(addressId) {
  return this.delete(endpoints.address(addressId));
}
