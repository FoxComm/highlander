// @flow

import _ from 'lodash';

import type { Address } from 'types/address';

// try find address in addresses list by comparing its content without `id` field
export function lookupAddressId(addresses: Array<Address>, address: ?Address): null|number {
  let addressId = null;

  if (address) {
    const sample = _.omit(address, 'id');

    _.some(addresses, nextAddress => {
      if (_.isEqual(_.omit(nextAddress, 'id'), sample)) {
        addressId = nextAddress.id;
        return true;
      }
    });
  }

  return addressId;
}
