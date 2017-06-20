
import _ from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import { eventTarget } from './customer-link';

const ActivityTitle = props => {
  const { addEventTarget, activity } = props;
  let recipient = null;
  if (addEventTarget) {
    const customer = activity.data.user || _.get(activity.data, 'order.customer');

    if (customer) {
      recipient = eventTarget(activity, customer);
    }
  }
  return (
    <span>
      {props.children}
      {recipient}.
    </span>
  );
};

ActivityTitle.propTypes = {
  children: PropTypes.node,
  activity: PropTypes.shape({
    context: PropTypes.object.isRequired
  }),
  addEventTarget: PropTypes.bool,
};

ActivityTitle.defaultProps = {
  addEventTarget: true,
};

export default ActivityTitle;
