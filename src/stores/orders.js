'use strict';

import { List } from 'immutable';
import BaseStore from './base-store';
import OrderConstants from '../constants/orders';

class OrderStore extends BaseStore {
  constructor() {
    super();
    this.changeEvent = 'change-orders';
    this.state = List([]);

    this.bindListener(OrderConstants.UPDATE_ORDERS, this.handleUpdateOrders);
    this.bindListener(OrderConstants.FAILED_ORDERS, this.handleFailedOrders);
    this.bindListener(OrderConstants.INSERT_ORDER, this.handleInsertOrder);
  }

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

  sort(field, order=1) {
    this.setState(
      this.state.sort((a, b) => {
        return (1 - 2 * order) * (a[field] < b[field] ? 1 : a[field] > b[field] ? -1 : 0);
      })
    );
  }

  handleUpdateOrders(action) {
    this.setState(List(action.orders));
  }

  handleFailedOrders(action) {
    console.error(action.errorMessage.trim());
  }

  handleInsertOrder(action) {
    const order = action.order;
    this.setState(this.insertIntoList(this.state, order, 'referenceNumber'));
  }
}

let orderStore = new OrderStore();
export default orderStore;
