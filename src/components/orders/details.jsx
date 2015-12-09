import React, { PropTypes } from 'react';
import _ from 'lodash';
import TotalsSummary from '../common/totals';
import CustomerInfo from './customer-info';
import OrderLineItems from './order-line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './order-shipping-method';
import Payments from './payments';
import Watchers from '../watchers/watchers';

const OrderDetails = props => {
  if (_.isEmpty(props.order.currentOrder)) {
    return <div className="fc-order-details"></div>;
  } else {
    const order = props.order.currentOrder;

    return (
      <div className="fc-order-details">
        <div className="fc-order-details-body">
          <div className="fc-order-details-main">
            <OrderLineItems {...props} />
            <OrderShippingAddress order={order} />
            <OrderShippingMethod {...props} />
            <Payments {...props} />
          </div>
          <div className="fc-order-details-aside">
            <TotalsSummary entity={order} title={order.title} />
            <CustomerInfo order={order} />
            <Watchers />
          </div>
        </div>
      </div>
    );
  }
};

OrderDetails.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.object
  })
};

export default OrderDetails;
