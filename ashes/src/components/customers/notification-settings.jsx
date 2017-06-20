import React from 'react';
import ContentBox from '../content-box/content-box';
import { SliderCheckbox } from 'components/core/checkbox';

export default class CustomerNotificationSettings extends React.Component {

  render() {
    return (
      <ContentBox title="Email & Notification Preferences" className="fc-customer-notification-settings">
        <div className="fc-grid fc-customer-settings-title-row">
          <div className="fc-col-md-1-1">
            <strong>Transactional Notifications</strong>
            <span className="fc-customer-notification-title-comment">&nbsp;(must select 1)</span>
          </div>
        </div>
        <div className="fc-customer-status-row">
          SMS Notifications
          <SliderCheckbox id="customerSmsNotifications" defaultChecked={ false } />
        </div>
        <div className="fc-customer-status-row">
          Email Notifications
          <SliderCheckbox id="customerEmailNotifications" defaultChecked={ true } />
        </div>
        <div className="fc-grid fc-customer-settings-title-row">
          <div className="fc-col-md-1-1">
            <strong>Marketing</strong>
          </div>
        </div>
        <div className="fc-customer-status-row">
          SMS Promotional Offers
          <SliderCheckbox id="customerSmsPromoOffers" defaultChecked={ true } />
        </div>
        <div className="fc-customer-status-row">
          Email Promotional Offers
          <SliderCheckbox id="customerEmailPromoOffers" defaultChecked={ true } />
        </div>
      </ContentBox>
    );
  }
}
