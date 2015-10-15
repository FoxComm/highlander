'use strict';

import React from 'react';
import OrderSummary from './summary';
import CustomerInfo from './customer-info';
import LineItems from './order-line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from '../shipping/shipping-method';
import OrderPayment from './payment';
import OrderStore from './../../stores/orders';

let OrderDetails = (props) => {
  return (
    <div className="fc-order-details">
      <div className="fc-order-details-body">
        <div className="fc-order-details-main">
          <LineItems
            entity={props.order}
            model={'order'} />
          <OrderShippingAddress order={props.order} />
          <OrderShippingMethod order={props.order} />
          <OrderPayment order={props.order} />
        </div>
        <div className="fc-order-details-aside">
          <OrderSummary order={props.order} />
          <CustomerInfo order={props.order} />
        </div>
      </div>
    </div>
  );
};

export default OrderDetails;
