'use strict';

import BaseStore from '../../lib/base-store';

class OrderStore extends BaseStore {
  get baseUri() { return '/orders'; }

  viewers() {
  }
}

export default new OrderStore();
