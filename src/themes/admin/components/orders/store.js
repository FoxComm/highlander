'use strict';

import BaseStore from '../../lib/base-store';

class OrderStore extends BaseStore {
  get baseUri() { return '/orders'; }
}

export default new OrderStore();
