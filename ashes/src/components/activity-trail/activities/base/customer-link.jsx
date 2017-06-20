
import React from 'react';
import PropTypes from 'prop-types';

import { Link } from 'components/link';

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

export function eventTarget(activity, customer) {
  if (!activity.data.admin && activity.context.userType == 'user' ) {
    return <span> for customer <CustomerLink customer={customer} /></span>;
  }
}

export default CustomerLink;
