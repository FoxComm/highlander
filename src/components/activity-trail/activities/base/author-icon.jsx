
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import UserInitials from '../../../users/initials';


const AuthorIcon = props => {
  const { activity } = props;

  const userType = _.get(activity, ['context', 'userType'], 'system');

  switch (userType) {
    case 'admin':
      return <UserInitials name={activity.data.admin.name} />;
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
