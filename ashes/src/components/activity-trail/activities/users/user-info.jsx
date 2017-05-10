
import React from 'react';
import PropTypes from 'prop-types';

const UserInfo = props => {
  let email = null;
  if (props.email) {
    email = [
      <dt>Email Address</dt>,
      <dd>{props.email}</dd>
    ];
  }

  let phoneNumber = null;
  if (props.phoneNumber) {
    phoneNumber = [
      <dt>Phone Number</dt>,
      <dd>{props.phoneNumber}</dd>
    ];
  }
  return (
    <dl className="fc-activity__customer-info">
      <dt>Name</dt>
      <dd>{props.name}</dd>
      {email}
      {phoneNumber}
    </dl>
  );
};

UserInfo.propTypes = {
  name: PropTypes.string.isRequired,
  email: PropTypes.string,
  phoneNumber: PropTypes.string,
};

export default UserInfo;
