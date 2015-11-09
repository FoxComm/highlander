'use strict';

import React, { PropTypes } from 'react';
import OrderSummary from './summary';
import CustomerInfo from './customer-info';
import OrderLineItems from './order-line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './order-shipping-method';
import OrderPayment from './payment';

const OrderDetails = props => {
  return (
    <div className="fc-order-details">
      <div className="fc-order-details-body">
        <div className="fc-order-details-main">
          <OrderLineItems {...props} />
          <OrderShippingAddress order={props.order.currentOrder} />
          <OrderShippingMethod {...props} />
          <OrderPayment {...props} />
        </div>
        <div className="fc-order-details-aside">
          <OrderSummary order={props.order.currentOrder} />
          <CustomerInfo order={props.order.currentOrder} />
        </div>
      </div>
    </div>
  );
};

OrderDetails.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.object
  })
};

export default OrderDetails;
