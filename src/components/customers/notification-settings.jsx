'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';

export default class CustomerNotificationSettings extends React.Component {

  render() {
    return (
      <ContentBox title="Email & Notification Preferences" className="fc-customer-notification-settings">
        Notification Settings
      </ContentBox>
    );
  }
}
