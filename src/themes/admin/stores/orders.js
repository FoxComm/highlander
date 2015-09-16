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

  process(model) {
    _.each(model.lineItems, lineItem => {
      lineItem.total = lineItem.qty * lineItem.price;
    });
  }

  setShippingAddress(orderId, addressId) {
    let uri = `${this.uri(orderId)}/shipping-address`;
    return Api.patch(uri, {addressId})
      .then((res) => {
        // update shipping addres for order in store
        this.fetch(orderId);
      });
  }
}

export default new OrderStore();
