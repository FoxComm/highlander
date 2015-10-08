'use strict';

import _ from 'lodash';
import BaseStore from '../lib/base-store';

class LineItemStore extends BaseStore {
  get baseUri() { return `${this.rootUri}/line-items`; }
  set uriRoot(uri) { this.rootUri = uri; }
}

export default new LineItemStore();
