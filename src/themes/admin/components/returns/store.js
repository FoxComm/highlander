'use strict';

import BaseStore from '../../lib/base-store';

class ReturnsStore extends BaseStore {
  get baseUri() {
    return '/returns';
  }
}

export default new ReturnsStore();
