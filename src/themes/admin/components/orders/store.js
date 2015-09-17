'use strict';

import _ from 'lodash';
import BaseStore from '../../lib/base-store';

class OrderStore extends BaseStore {
  get statuses() {
    return {
      cart: 'Cart',
      remorseHold: 'Remorse Hold',
      manualHold: 'Manual Hold',
      fraudHold: 'Fraud Hold',
      fulfillmentStarted: 'Fulfillment Started',
      canceled: 'Canceled',
      partiallyShipped: 'Partially Shipped',
      shipped: 'Shipped'
    };
  }

  get selectableStatusList() { return ['remorseHold', 'manualHold', 'fraudHold', 'fulfillmentStarted', 'canceled']; }
  get editableStatusList() { return ['remorseHold', 'manualHold', 'fraudHold', 'fulfillmentStarted']; }
  get holdStatusList() { return ['remorseHold', 'manualHold', 'fraudHold']; }

  get baseUri() { return '/orders'; }

  process(model) {
    _.each(model.lineItems, lineItem => {
      lineItem.total = lineItem.quantity * lineItem.price;
    });
  }
}

export default new OrderStore();
