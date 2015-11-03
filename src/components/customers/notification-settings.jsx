'use strict';

import React from 'react';
import ContentBox from '../content-box/content-box';
import { SliderCheckbox } from '../checkbox/checkbox';

export default class CustomerNotificationSettings extends React.Component {

  render() {
    return (
      <ContentBox title="Email & Notification Preferences" className="fc-customer-notification-settings">
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              SMS Notifications
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerSmsNotifications" defaultChecked={ false } />
            </div>
        </div>
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              Email Notifications
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerEmailNotifications" defaultChecked={ true } />
            </div>
        </div>
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              SMS Promotional Offers
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerSmsPromoOffers" defaultChecked={ true } />
            </div>
        </div>
        <div className="fc-grid fc-customer-status-row">
            <div className="fc-col-md-2-3">
              Email Promotional Offers
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerEmailPromoOffers" defaultChecked={ true } />
            </div>
        </div>
      </ContentBox>
    );
  }
}
