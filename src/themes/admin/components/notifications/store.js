'use strict';

import BaseStore from '../../lib/base-store';

class NotificationStore extends BaseStore {
  get baseUri() { return '/orders/:order/notifications'; }
}

export default new NotificationStore();
