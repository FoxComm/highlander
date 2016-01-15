
import React, { PropTypes } from 'react';
import OrderLink from './order-link';

const OrderTarget = ({order}) => {
  return (
    <span>
      {order.title}
      &nbsp;
      <OrderLink order={order} />
    </span>
  );
};

OrderTarget.propTypes = {
  order: PropTypes.shape({
    title: PropTypes.string,
    referenceNumber: PropTypes.string.isRequired,
  }),
};

export default OrderTarget;
