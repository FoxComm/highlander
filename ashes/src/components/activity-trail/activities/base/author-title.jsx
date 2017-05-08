
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';

// components
import CustomerLink from './customer-link';

const AuthorTitle = props => {
  const { activity } = props;
  const userType = _.get(activity, ['context', 'userType'], 'system');
  const adminName = _.get(activity, ['data', 'admin', 'name']);

  switch (userType) {
    case 'user':
      if (!_.isEmpty(adminName)) {
        return <span>{adminName}</span>;
      } else if (activity.data.user) {
        return <CustomerLink customer={activity.data.user} />;
      } else {
        // @TODO: should be `Unrecognised Admin`, but backend is not ready to pass proper userType for system actions
        // so now correct option here is `FoxCommerce`
        return <span>FoxCommerce</span>;
      }
    case 'guest':
      if (activity.data.user) {
        return <CustomerLink customer={activity.data.user} />;
      }
      return <span>The Guest</span>;
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
