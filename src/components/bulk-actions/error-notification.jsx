import React from 'react';
import Notification from './notification';


export default function ErrorNotification(props) {
  return <Notification resultType="error" {...props} />;
}
