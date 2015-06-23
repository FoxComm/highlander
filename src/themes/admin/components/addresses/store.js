'use strict';

import BaseStore from '../../lib/base-store';

class AddressStore extends BaseStore {
  get baseUri() { return `${this.rootUri}/addresses`; }
  set uriRoot(uri) { this.rootUri = uri; }
}

export default new AddressStore();
