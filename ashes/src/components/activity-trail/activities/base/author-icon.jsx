
// libs
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import classNames from 'classnames';

// components
import DetailedInitials from '../../../user-initials/detailed-initials';
import Icon from 'components/icon/icon';

// styles
import s from './author-icon.css';

const AuthorIcon = props => {
  const { activity, className } = props;
  const userType = _.get(activity, ['context', 'userType'], 'system');
  const adminName = _.get(activity, ['data', 'admin', 'name']);

  switch (userType) {
    case 'admin':
      if (!_.isEmpty(adminName)) {
        return <DetailedInitials name={adminName} className={className} />;
      } else {
        return <Icon name="fox" className={classNames(s.systemIcon, className)} />;
      }
    case 'account':
    case 'user':
    case 'customer':
      return (
        <div className={classNames('fc-activity__customer-icon', className)}>
          <i className="icon-customer"></i>
        </div>
      );
    default:
      return <Icon name="fox" className={classNames(s.systemIcon, className)} />;
  }
};

AuthorIcon.propTypes = {
  activity: PropTypes.shape({
    data: PropTypes.object,
    context: PropTypes.object,
  }).isRequired,
};

export default AuthorIcon;
