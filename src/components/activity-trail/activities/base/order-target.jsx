
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

const OrderTarget = ({order}) => {
  return (
    <span>
      {order.title}
      &nbsp;
      <Link className="fc-activity__link" to="order" params={{order: order.referenceNumber}}>{order.referenceNumber}</Link>
    </span>
  );
};

OrderTarget.propTypes = {
  order: PropTypes.shape({
    referenceNumber: PropTypes.string.isRequired,
  }),
};

export default OrderTarget;
