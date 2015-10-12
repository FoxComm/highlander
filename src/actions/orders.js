'use strict';

import Api from '../lib/api';
import AshesDispatcher from '../lib/dispatcher';
import OrderConstants from '../constants/orders';
import { List } from 'immutable';

class OrderActions {
  updateOrders(orders) {
    AshesDispatcher.handleAction({
      actionType: OrderConstants.UPDATE_ORDERS,
      orders: orders
    });
  }

  failedOrders(errorMessage) {
    AshesDispatcher.handleAction({
      actionType: OrderConstants.FAILED_ORDERS,
      errorMessage: errorMessage
    });
  }

  insertOrder(order) {
    AshesDispatcher.handleAction({
      actionType: OrderConstants.INSERT_ORDER,
      order: order
    });
  }

  updateOrderStatus(refNum, status) {
    return Api.patch(`/orders/${refNum}`)
      .then((order) => {
        this.insertOrder(order);
      })
      .catch((err) => {
        this.failedOrders(err);
      });
  }

  fetchOrders() {
    return Api.get('/orders')
      .then((orders) => {
        this.updateOrders(List(orders));
      })
      .catch((err) => {
        this.failedOrders(err);
      });
  }

  fetchOrder(refNum) {
    return Api.get(`/orders/${refNum}`)
      .then((order) => {
        this.insertOrder(order);
      })
      .catch((err) => {
        this.failedOrders(err);
      });
  }

  _callShippingAddressMethod(method, refNum, body = void 0) {
    let uri = `/orders/${refNum}/shipping-address`;
    return Api[method](uri, body)
      .then((res) => {
        // update shipping address for order in store
        this.fetchOrder(refNum);
      });
  }

  setShippingAddress(refNum, addressId) {
    this._callShippingAddressMethod('patch', refNum, {addressId});
  }

  removeShippingAddress(refNum) {
    this._callShippingAddressMethod('delete', refNum);
  }
}

export default new OrderActions();
