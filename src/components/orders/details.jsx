// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import TotalsSummary from 'components/common/totals';
import CustomerCard from 'components/customer-card/customer-card';
import OrderLineItems from './order-line-items';
import OrderShippingAddress from './shipping-address';
import OrderShippingMethod from './order-shipping-method';
import Payments from './payments';
import OrderDiscounts from './order-discounts';
import OrderCoupons from './order-coupons';
import Watchers from '../watchers/watchers';

const OrderDetails = props => {
  const {order} = props;
  const {currentOrder} = order;

  if (_.isEmpty(currentOrder)) {
    return <div className="fc-order-details"></div>;
  }

  return (
    <div className="fc-order-details">
      <div className="fc-order-details-body">
        <div className="fc-order-details-main">
          <OrderLineItems order={currentOrder} />
          <OrderDiscounts order={currentOrder} />
          <OrderShippingAddress isCart={false} order={currentOrder} />
          <OrderShippingMethod isCart={false} {...props} />
          <OrderCoupons isCart={false} order={currentOrder} />
          <Payments {...props} />
        </div>
        <div className="fc-order-details-aside">
          <TotalsSummary entity={currentOrder} title={currentOrder.title} />
          <CustomerCard customer={currentOrder.customer} />
          <Watchers entity={{entityId: currentOrder.referenceNumber, entityType: 'orders'}} />
        </div>
      </div>
    </div>
  );
};

OrderDetails.propTypes = {
  order: PropTypes.shape({
    currentOrder: PropTypes.object,
    validations: PropTypes.object,
  }),
  checkout: PropTypes.func,
};

OrderDetails.defaultProps = {
  checkout: _.noop,
};

export default OrderDetails;
