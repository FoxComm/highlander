'use strict';

import BaseStore from '../lib/base-store';

class CustomerStore extends BaseStore {
  get baseUri() { return '/customers'; }

  fetch(id) {
    return this._lastFetch = super.fetch(id);
  }

  identity(customer) {
    return customer.id;
  }
}

export default new CustomerStore();
