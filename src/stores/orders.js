'use strict';

import { List } from 'immutable';
import BaseStore from './base-store';
import OrderConstants from '../constants/orders';
import LineItemConstants from '../constants/line-items';

class OrderStore extends BaseStore {
  constructor() {
    super();
    this.changeEvent = 'change-orders';
    this.state = List([]);

    this.bindListener(OrderConstants.UPDATE_ORDERS, this.handleUpdateOrders);
    this.bindListener(OrderConstants.FAILED_ORDERS, this.handleFailedOrders);
    this.bindListener(OrderConstants.INSERT_ORDER, this.handleInsertOrder);
    this.bindListener(LineItemConstants.ORDER_LINE_ITEM_SUCCESS, this.handleInsertOrder);
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

  handleUpdateOrders(action) {
    this.setState(List(action.orders));
  }

  handleFailedOrders(action) {
    console.error(action.errorMessage.trim());
  }

  handleInsertOrder(action) {
    const order = action.order;
    this.setState(this.insertIntoList(
      this.state, order, null,
      item => item.referenceNumber === order.referenceNumber
    ));
  }

  _callShippingAddressMethod(method, refNum, body = void 0) {
    let uri = `${this.uri(refNum)}/shipping-address`;
    return Api[method](uri, body)
      .then((res) => {
        // update shipping address for order in store
        this.fetch(refNum);
      });
  }

  setShippingAddress(refNum, addressId) {
    this._callShippingAddressMethod('patch', refNum, {addressId});
  }

  removeShippingAddress(refNum) {
    this._callShippingAddressMethod('delete', refNum);
  }
}

let orderStore = new OrderStore();
export default orderStore;
