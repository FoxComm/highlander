"use strict";

import ParameterizedStore from '../lib/parameterized-store';

class AddressStore extends ParameterizedStore {
  baseUri(customerId) {
    return `/customers/${customerId}/addresses`;
  }
}

export default new AddressStore();
