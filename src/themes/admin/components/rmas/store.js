'use strict';

import BaseStore from '../../lib/base-store';

class RmaStore extends BaseStore {
  get baseUri() {
    return '/returns';
  }
}

export default new RmaStore();
