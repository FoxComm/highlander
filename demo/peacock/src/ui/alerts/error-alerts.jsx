// @flow weak

import React from 'react';
import s from './error-alerts.css';
import WingsErrorAlerts from '@foxcomm/wings/lib/ui/alerts/error-alerts';


const ErrorAlerts = (props) => {
  return <WingsErrorAlerts {...props} className={s.block} />
};

export default ErrorAlerts;
