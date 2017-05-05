
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

// components
import DetailedInitials from '../../../user-initials/detailed-initials';


const AuthorIcon = props => {
  const { activity } = props;
  const userType = _.get(activity, ['context', 'userType'], 'system');
  const adminName = _.get(activity, ['data', 'admin', 'name']);

  switch (userType) {
    case 'admin':
      if (!_.isEmpty(adminName)) {
        return <DetailedInitials name={adminName} />;
      } else {
        return <div className="fc-activity__system-icon"></div>;
      }
    case 'account':
    case 'user':
    case 'customer':
      return (
        <div className="fc-activity__customer-icon">
          <i className="icon-customer"></i>
        </div>
      );
    default:
      return <div className="fc-activity__system-icon"></div>;
  }
};

AuthorIcon.propTypes = {
  activity: PropTypes.shape({
    data: PropTypes.object,
    context: PropTypes.object,
  }).isRequired,
};

export default AuthorIcon;
