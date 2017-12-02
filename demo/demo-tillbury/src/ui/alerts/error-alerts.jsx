// @flow weak

import React from 'react';
import classnames from 'classnames';
import s from './error-alerts.css';
import WingsErrorAlerts from '@foxcommerce/wings/lib/ui/alerts/error-alerts';


const ErrorAlerts = (props) => {
  const className = classnames(s.block, props.className);
  return <WingsErrorAlerts {...props} className={className} />;
};

export default ErrorAlerts;
