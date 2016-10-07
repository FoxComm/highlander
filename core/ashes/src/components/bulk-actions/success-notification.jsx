import React from 'react';
import Notification from './notification';


export default function SuccessNotification(props) {
  return <Notification resultType="success" {...props} />;
}
