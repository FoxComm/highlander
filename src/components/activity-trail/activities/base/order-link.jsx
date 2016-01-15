
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

const OrderLink = ({order}) => {
  return (
    <Link className="fc-activity__link" to="order" params={{order: order.referenceNumber}}>
      {order.referenceNumber}
    </Link>
  );
};

OrderLink.propTypes = {
  order: PropTypes.shape({
    title: PropTypes.string,
    referenceNumber: PropTypes.string.isRequired,
  }),
};

export default OrderLink;
