
import _ from 'lodash';
import React, { PropTypes } from 'react';
import { eventTarget } from './customer-link';

const ActivityTitle = props => {
  const { addEventTarget, activity } = props;
  let recipient = null;
  if (addEventTarget) {
    const customer = activity.data.customer || _.get(activity.data, 'order.customer');

    if (customer) {
      recipient = eventTarget(activity.context, customer);
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
