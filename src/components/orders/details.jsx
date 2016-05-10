// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import TotalsSummary from '../common/totals';
import Checkout from './checkout';
import CustomerInfo from './customer-info';
import Messages from './messages';
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

  const isCart = _.isEqual(currentOrder.orderState, 'cart');

  const {
    errors,
    warnings,
    itemsStatus,
    shippingAddressStatus,
    shippingMethodStatus,
    paymentMethodStatus
  } = order.validations;

  return (
    <div className="fc-order-details">
      <div className="fc-order-details-body">
        <div className="fc-order-details-main">
          <OrderLineItems isCart={isCart} status={itemsStatus} {...props} />
          <OrderDiscounts isCart={isCart} order={currentOrder} />
          <OrderShippingAddress isCart={isCart} status={shippingAddressStatus} order={currentOrder} />
          <OrderShippingMethod isCart={isCart} status={shippingMethodStatus} {...props} />
          <OrderCoupons isCart={isCart} order={currentOrder} />
          <Payments isCart={isCart} status={paymentMethodStatus} {...props} />
          {isCart && <Checkout checkout={props.checkout} order={order} />}
        </div>
        <div className="fc-order-details-aside">
          <Messages errors={errors} warnings={warnings} />
          <TotalsSummary entity={currentOrder} title={currentOrder.title} />
          <CustomerInfo order={currentOrder} />
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
