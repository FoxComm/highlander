'use strict';

import BaseStore from '../../lib/base-store';
import Api from '../../lib/api';

class AddressStore extends BaseStore {
  get baseUri() { return '/addresses'; }

  create(data) {
    Api.post(this.uri(), data)
      .then((res) => { this.update(res); })
      .catch((err) => { this.fetchError(err); });
  }
}

export default new AddressStore();
