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
        <div className="fc-grid fc-grid-gutter">
            <div className="fc-col-md-2-3">
              SMS Notifications
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerSmsNotifications" defaultChecked={ false } />
            </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
            <div className="fc-col-md-2-3">
              Email Notifications
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerEmailNotifications" defaultChecked={ true } />
            </div>
        </div>
        <div className="fc-grid fc-customer-settings-title-row">
          <div className="fc-col-md-1-1">
            <strong>Marketing</strong>
          </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
            <div className="fc-col-md-2-3">
              SMS Promotional Offers
            </div>
            <div className="fc-col-md-1-3">
              <SliderCheckbox className="fc-right" id="customerSmsPromoOffers" defaultChecked={ true } />
            </div>
        </div>
        <div className="fc-grid fc-grid-gutter">
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
