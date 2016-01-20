
import React, { PropTypes } from 'react';
import { Link } from '../../../link';

const CustomerLink = props => {
  return (
    <Link className="fc-activity__link" to="customer" params={{customerId: props.customer.id}}>
      {props.customer.name}
    </Link>
  );
};

CustomerLink.propTypes = {
  customer: PropTypes.shape({
    name: PropTypes.string.isRequired,
    id: PropTypes.number.isRequired,
  }),
};

export default CustomerLink;
