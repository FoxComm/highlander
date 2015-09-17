'use strict';

import React from 'react';
import OrderSummary from './summary';
import CustomerInfo from './customer-info';
import OrderLineItems from './line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './shipping-method';
import OrderPayment from './payment';
import OrderStore from './store';
import { dispatch } from '../../lib/dispatcher';
import Api from '../../lib/api';

export default class OrderDetails extends React.Component {
  constructor(props) {
    super(props);
  }

  updateLineItems(data) {
    Api.post(`/orders/${this.props.order.referenceNumber}/line-items`, data)
      .then((res) => {
        OrderStore.update(res);
      })
      .catch((err) => {
        console.error(err);
      });
  }

  render() {
    let
      order     = this.props.order,
      actions   = null;

    return (
      <div className="order-details">
        <div className="order-details-body">
          <div className="order-details-main">
            <OrderLineItems order={order} onChange={this.updateLineItems.bind(this)} />
            <OrderShippingAddress order={order} />
            <OrderShippingMethod order={order} />
            <OrderPayment order={order} />
          </div>
          <div className="order-details-aside">
            <OrderSummary order={order} />
            <CustomerInfo order={order} />
          </div>
        </div>
      </div>
    );
  }
}

OrderDetails.propTypes = {
  order: React.PropTypes.object
};
