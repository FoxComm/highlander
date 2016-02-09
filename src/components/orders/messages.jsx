import React, { PropTypes } from 'react';
import classnames from 'classnames';
import _ from 'lodash';

import Alert from '../alerts/alert';

const formatMessage = message => {
  if (message.indexOf('empty cart') != -1) {
    return 'Cart is empty.';
  } else if (message.indexOf('no shipping address') != -1) {
    return 'No shipping address applied.';
  } else if (message.indexOf('invalid shipping method') != -1) {
    return 'Shipping method is not valid.';
  } else if (message.indexOf('has no shipping method') != -1 ) {
    return 'No shipping method applied.';
  } else if (message.indexOf('no payment method') != -1) {
    return 'No payment method applied.';
  }

  return message;
};

const Messages = props => {
  const { errors, warnings } = props;

  const errorAlerts = _.map(errors, e => {
    return <Alert type={Alert.ERROR}>{formatMessage(e)}</Alert>;
  });

  const warningAlerts = _.map(warnings, w => {
    return <Alert type={Alert.WARNING}>{formatMessage(w)}</Alert>;
  });

  const className = classnames('fc-order-messages', {
    '_empty': errorAlerts.length + warningAlerts.length == 0
  });

  return (
    <div className={className}>
      {errorAlerts}
      {warningAlerts}
    </div>
  );
};

Messages.propTypes = {
  errors: PropTypes.array,
  warnings: PropTypes.array
};

Messages.defaultProps = {
  errors: [],
  warnings: []
};

export default Messages;
