'use strict';

import _ from 'lodash';
import Api from '../lib/api';
import BaseStore from '../lib/base-store';

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

  identity(order) {
    return order.referenceNumber;
  }

  process(model) {
    _.each(model.lineItems, lineItem => {
      lineItem.total = lineItem.quantity * lineItem.price;
    });
  }

  setShippingAddress(refNum, addressId) {
    let uri = `${this.uri(refNum)}/shipping-address`;
    return Api.patch(uri, {addressId})
      .then((res) => {
        // update shipping addres for order in store
        this.fetch(refNum);
      });
  }
}

export default new OrderStore();
