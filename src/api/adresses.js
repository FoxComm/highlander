// @namespace AddressesResponse
// @inherits Response
// @option result: Address[]


// @namespace FoxApi

import * as endpoints from '../endpoints';

// @method getAddresses(options: Query options): Promise<AddressesResponse>
// Choices for `options.sortBy` parameter:
// `id` `regionId` `name` `address1` `address2` `city` `zip` `isDefaultShipping` `phoneNumber` `deletedAt` `region_id` `region_countryId` `region_name` `region_abbreviation`
export function getAddresses(options) {
  return this.get(endpoints.addresses, options);
}

// @method getAddress(addressId: Number): Promise<Address>
export function getAddress(addressId) {
  return this.get(endpoints.address(addressId))
}

// @method addAddress(address: CreateAddressPayload): Promise<Address>
// Adds new address
export function addAddress(address) {
  return this.post(endpoints.addresses, address)
}

// @method editAddress(addressId: Number, address: UpdateAddressPayload): Promise<Address>
// Updates selected address
export function editAddress(addressId, address) {
  return this.patch(endpoints.address(addressId), address);
}

// @method setAddressAsDefaut(addressId: Number): Promise<Address>
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
