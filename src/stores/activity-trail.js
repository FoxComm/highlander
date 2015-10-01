'use strict';

import BaseStore from '../../lib/base-store';

class ActivityTrailStore extends BaseStore {
  get baseUri() { return `${this.rootUri}/activity-trail`; }
  set uriRoot(uri) { this.rootUri = uri; }
}

export default new ActivityTrailStore();
