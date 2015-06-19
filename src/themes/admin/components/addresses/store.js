'use strict';

import BaseStore from '../../lib/base-store';

class AddressStore extends BaseStore {
  get baseUri() { return '/addresses'; }
}

export default new AddressStore();
