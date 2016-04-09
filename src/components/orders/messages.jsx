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
  } else if (message.indexOf('has no shipping method') != -1) {
    return 'No shipping method applied.';
  } else if (message.indexOf('no payment method') != -1) {
    return 'No payment method applied.';
  } else if (message.indexOf('insufficient funds') != -1) {
    return 'Insufficient funds.';
  }

  return message;
};

const Messages = props => {
  const { errors, warnings } = props;

  const errorAlerts = _.map(errors, e => {
    const message = formatMessage(e);

    return <Alert type={Alert.ERROR} key={_.kebabCase(message)}>{message}</Alert>;
  });

  const warningAlerts = _.map(warnings, w => {
    const message = formatMessage(w);

    return <Alert type={Alert.WARNING} key={_.kebabCase(message)}>{message}</Alert>;
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
