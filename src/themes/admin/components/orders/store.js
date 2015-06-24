'use strict';

import BaseStore from '../../lib/base-store';

class OrderStore extends BaseStore {
  get statuses() {
    return {
      cart: 'Cart',
      remorseHold: 'Remorse Hold',
      manualHold: 'Manual Hold',
      fraudHold: 'Fraud Hold',
      fulfillmentHold: 'Fulfillment Hold',
      canceled: 'Canceled',
      partiallyShipped: 'Partially Shipped',
      shipped: 'Shipped'
    };
  }

  get selectableStatusList() { return ['remorseHold', 'manualHold', 'fraudHold', 'fulfillmentHold', 'canceled']; }
  get editableStatusList() { return ['remorseHold', 'manualHold', 'fraudHold']; }

  get baseUri() { return '/orders'; }
}

export default new OrderStore();
