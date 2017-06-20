import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import _ from 'lodash';

import Alert from 'components/core/alert';
import AutoScroll from 'components/utils/auto-scroll';

import s from './cart.css';

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
  } else if (message.indexOf('stock item') != -1) {
    return 'Not enough items in stock.';
  } else if (message.indexOf('insufficient funds') != -1) {
    return 'Insufficient funds.';
  }

  return message;
};

const Messages = props => {
  const { errors, warnings } = props;

  const errorAlerts = _.map(errors, e => {
    const message = formatMessage(e);

    return <Alert className={s.alert} type={Alert.ERROR} key={_.kebabCase(message)}>{message}</Alert>;
  });

  const warningAlerts = _.map(warnings, w => {
    const message = formatMessage(w);

    return <Alert className={s.alert} type={Alert.WARNING} key={_.kebabCase(message)}>{message}</Alert>;
  });

  const className = classnames('fc-order-messages', {
    '_empty': errorAlerts.length + warningAlerts.length === 0
  });

  const scrollToMessages = errorAlerts.length || warningAlerts.length ? <AutoScroll /> : null;

  return (
    <div className={className}>
      {scrollToMessages}
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
