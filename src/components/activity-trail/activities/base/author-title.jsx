
// libs
import _ from 'lodash';
import React, { PropTypes } from 'react';

// components
import CustomerLink from './customer-link';

const AuthorTitle = props => {
  const { activity } = props;

  const userType = _.get(activity, ['context', 'userType'], 'system');

  switch (userType) {
    case 'admin':
      return <span>{activity.data.admin.name}</span>;
    case 'customer':
      if (activity.data.customer) {
        return <CustomerLink customer={activity.data.customer} />;
      }
      return <span>The customer</span>;
    default:
      return <span>FoxCommerce</span>;
  }
};

AuthorTitle.propTypes = {
  activity: PropTypes.shape({
    data: PropTypes.object,
    context: PropTypes.object,
  }).isRequired,
};

export default AuthorTitle;
