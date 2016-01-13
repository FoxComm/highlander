
import React, { PropTypes } from 'react';

const CustomerInfo = props => {
  return (
    <dl className="fc-activity__customer-info">
      <dt>Name</dt>
      <dd>{props.name}</dd>
      <dt>Email Address</dt>
      <dd>{props.email}</dd>
      <dt>Phome Number</dt>
      <dd>{props.phoneNumber}</dd>
    </dl>
  );
};

CustomerInfo.propTypes = {
  name: PropTypes.string,
  email: PropTypes.string,
  phoneNumber: PropTypes.string,
};

export default CustomerInfo;
