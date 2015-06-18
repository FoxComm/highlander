'use strict';

import BaseStore from '../../lib/base-store';

class NotificationStore extends BaseStore {
  get baseUri() { return '/order/:order/notifications'; }
}

export default new NotificationStore();
