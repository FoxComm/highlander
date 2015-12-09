
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

const OrderTarget = ({order}) => {
  return (
    <span>
      {order.title}
      &nbsp;
      <Link to="order-details" params={{order: order.referenceNumber}}>{order.referenceNumber}</Link>
    </span>
  );
};

OrderTarget.propTypes = {
  order: PropTypes.shape({
    referenceNumber: PropTypes.string.isRequired,
  }),
};

export default OrderTarget;
