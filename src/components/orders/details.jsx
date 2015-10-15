'use strict';

import React from 'react';
import OrderSummary from './summary';
import CustomerInfo from './customer-info';
import LineItems from './order-line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './shipping-method';
import OrderPayment from './payment';
import OrderStore from './../../stores/orders';

export default class OrderDetails extends React.Component {
  static propTypes = {
    order: React.PropTypes.object
  };

  render() {
    return (
      <div className="fc-order-details">
        <div className="fc-order-details-body">
          <div className="fc-order-details-main">
            <LineItems
              entity={this.props.order}
              model={'order'} />
            <OrderShippingAddress order={this.props.order} />
            <OrderShippingMethod order={this.props.order} />
            <OrderPayment order={this.props.order} />
          </div>
          <div className="fc-order-details-aside">
            <OrderSummary order={this.props.order} />
            <CustomerInfo order={this.props.order} />
          </div>
        </div>
      </div>
    );
  }
}
