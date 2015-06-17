'use strict';

import BaseStore from '../../lib/base-store';

class CustomerStore extends BaseStore {
  get baseUri() { return '/customers'; }
}

export default new CustomerStore();
